package com.csa.official.modules.ai;

import com.csa.official.modules.sys.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Exercises the real transaction interceptor and JDBC writes without starting the web application. */
class KnowledgeTransactionTest {

    @Test
    void laterChunkFailureRollsBackDocumentAndEarlierChunksWithoutLosingCommittedData() {
        try (var context = new AnnotationConfigApplicationContext(TransactionConfig.class)) {
            DataSource source = context.getBean(DataSource.class);
            JdbcTemplate db = context.getBean(JdbcTemplate.class);
            try {
                new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V7__ai_knowledge_and_runs.sql")).execute(source);
                KnowledgeService service = context.getBean(KnowledgeService.class);
                assertThat(AopUtils.isAopProxy(service)).isTrue();

                User manager = new User();
                manager.setId(1L);
                manager.setRoleLevel(3);
                var committed = service.create(
                        new AiModels.DocumentRequest("Committed", "One existing chunk", "fixture", 1), manager);

                // The first new chunk is valid; the second fails inside the same proxied invocation.
                db.execute("ALTER TABLE ai_chunk ADD CONSTRAINT test_reject_later_chunk CHECK (chunk_index < 1)");
                assertThatThrownBy(() -> service.create(
                        new AiModels.DocumentRequest("Must roll back", "x".repeat(1000), "fixture", 1), manager))
                        .isInstanceOf(DataIntegrityViolationException.class);

                // A separate template obtains a connection after the failed transaction has ended.
                JdbcTemplate verification = new JdbcTemplate(source);
                assertThat(verification.queryForObject("SELECT COUNT(*) FROM ai_document", Integer.class)).isEqualTo(1);
                assertThat(verification.queryForObject("SELECT COUNT(*) FROM ai_chunk", Integer.class)).isEqualTo(1);
                assertThat(verification.queryForObject("SELECT id FROM ai_document", String.class)).isEqualTo(committed.id());
                assertThat(verification.queryForObject("SELECT document_id FROM ai_chunk", String.class)).isEqualTo(committed.id());
            } finally {
                db.execute("SHUTDOWN");
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TransactionConfig {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:knowledge-tx-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource source) {
            return new JdbcTemplate(source);
        }

        @Bean
        DataSourceTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }

        @Bean
        KnowledgeService knowledgeService(JdbcTemplate db) {
            return new KnowledgeService(db, mock(ModelGateway.class), new AiProperties(), new ObjectMapper());
        }
    }
}
