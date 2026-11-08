package com.csa.official.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Run with `mvn -Dit.containers=true test` when Docker is available.
 * The ordinary unit-test command skips this test so local work does not require Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfSystemProperty(named = "it.containers", matches = "true")
class FlywayMySqlRedisIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("csa_integration")
            .withUsername("csa_integration")
            .withPassword(UUID.randomUUID().toString());

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @Test
    void initializesEmptyMySqlUpgradesV1DataAndRoundTripsRedis() throws Exception {
        Flyway v1 = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .cleanDisabled(true)
                .load();

        v1.migrate();

        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet users = statement.executeQuery("SELECT COUNT(*) FROM sys_user")) {
                assertThat(users.next()).isTrue();
                assertThat(users.getInt(1)).isZero();
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO sys_user
                        (username, password, email, student_id, role_level, position_type, balance, deleted)
                    VALUES (?, ?, ?, ?, 0, 0, 0.00, 0)
                    """)) {
                insert.setString(1, "legacy_student");
                insert.setString(2, "test-only-password-hash");
                insert.setString(3, " Legacy.Student@Example.Invalid ");
                insert.setString(4, " 20260001 ");
                assertThat(insert.executeUpdate()).isEqualTo(1);
            }
        }

        Flyway latest = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();
        latest.migrate();

        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet tables = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = 'sys_user'")) {
                assertThat(tables.next()).isTrue();
                assertThat(tables.getInt(1)).isEqualTo(1);
            }
            try (ResultSet history = statement.executeQuery(
                    "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1")) {
                assertThat(history.next()).isTrue();
                assertThat(history.getString(1)).isEqualTo("8");
            }
            try (ResultSet aiTables = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name IN ('ai_document', 'ai_chunk', 'ai_run', 'ai_evaluation')
                    """)) {
                assertThat(aiTables.next()).isTrue();
                assertThat(aiTables.getInt(1)).isEqualTo(4);
            }
            try (ResultSet aiIndexes = statement.executeQuery("""
                    SELECT COUNT(DISTINCT index_name)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND index_name IN ('idx_ai_document_role', 'idx_ai_chunk_document',
                                         'idx_ai_run_owner', 'idx_ai_evaluation_owner')
                    """)) {
                assertThat(aiIndexes.next()).isTrue();
                assertThat(aiIndexes.getInt(1)).isEqualTo(4);
            }
            try (PreparedStatement document = connection.prepareStatement("""
                    INSERT INTO ai_document (id, title, source, min_role_level, chunk_count, owner_id)
                    VALUES ('integration-document', 'Knowledge fixture', 'integration', 3, 1, 60001)
                    """)) {
                assertThat(document.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement chunk = connection.prepareStatement("""
                    INSERT INTO ai_chunk (id, document_id, chunk_index, content, embedding_json, embedding_model)
                    VALUES ('integration-document:0', 'integration-document', 0, 'RAG evidence', '[1.0,0.0]', 'fixture')
                    """)) {
                assertThat(chunk.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement run = connection.prepareStatement("""
                    INSERT INTO ai_run (id, owner_id, result_json)
                    VALUES ('integration-run', 60001, '{"mode":"offline"}')
                    """)) {
                assertThat(run.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement report = connection.prepareStatement("""
                    INSERT INTO ai_evaluation (id, owner_id, report_json)
                    VALUES ('integration-evaluation', 60001, '{"evaluatorVersion":"rules-v1","dataset":[]}')
                    """)) {
                assertThat(report.executeUpdate()).isEqualTo(1);
            }
            try (ResultSet evidence = statement.executeQuery("""
                    SELECT c.content, c.embedding_model, d.min_role_level
                    FROM ai_chunk c JOIN ai_document d ON d.id = c.document_id
                    WHERE d.id = 'integration-document'
                    """)) {
                assertThat(evidence.next()).isTrue();
                assertThat(evidence.getString("content")).isEqualTo("RAG evidence");
                assertThat(evidence.getString("embedding_model")).isEqualTo("fixture");
                assertThat(evidence.getInt("min_role_level")).isEqualTo(3);
            }
            try (ResultSet report = statement.executeQuery("""
                    SELECT report_json FROM ai_evaluation
                    WHERE id = 'integration-evaluation' AND owner_id = 60001
                    """)) {
                assertThat(report.next()).isTrue();
                assertThat(report.getString(1)).contains("rules-v1", "dataset");
            }
            try (ResultSet reviewIndex = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'biz_resume'
                      AND index_name = 'idx_resume_status_update'
                    """)) {
                assertThat(reviewIndex.next()).isTrue();
                assertThat(reviewIndex.getInt(1)).isEqualTo(2);
            }
            try (ResultSet fileUsageTable = statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = 'sys_file_usage'")) {
                assertThat(fileUsageTable.next()).isTrue();
                assertThat(fileUsageTable.getInt(1)).isEqualTo(1);
            }
            try (ResultSet contributionColumns = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'sys_contribution_log'
                      AND column_name IN ('source', 'awarded_by')
                    """)) {
                assertThat(contributionColumns.next()).isTrue();
                assertThat(contributionColumns.getInt(1)).isEqualTo(2);
            }
            try (ResultSet resumeVersion = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'biz_resume'
                      AND column_name = 'version'
                      AND column_default = '0'
                    """)) {
                assertThat(resumeVersion.next()).isTrue();
                assertThat(resumeVersion.getInt(1)).isEqualTo(1);
            }
            try (ResultSet resumeVersionIndex = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'biz_resume'
                      AND index_name = 'idx_resume_user_version'
                    """)) {
                assertThat(resumeVersionIndex.next()).isTrue();
                assertThat(resumeVersionIndex.getInt(1)).isEqualTo(2);
            }
            try (ResultSet migratedUser = statement.executeQuery("""
                    SELECT email, student_id, account_status, session_version
                    FROM sys_user
                    WHERE username = 'legacy_student'
                    """)) {
                assertThat(migratedUser.next()).isTrue();
                assertThat(migratedUser.getString("email"))
                        .isEqualTo("legacy.student@example.invalid");
                assertThat(migratedUser.getString("student_id")).isEqualTo("20260001");
                assertThat(migratedUser.getString("account_status")).isEqualTo("ACTIVE");
                assertThat(migratedUser.getLong("session_version")).isZero();
            }

            try (PreparedStatement insertResume = connection.prepareStatement("""
                    INSERT INTO biz_resume (user_id, status, version, deleted)
                    VALUES (?, 0, 0, 0)
                    """)) {
                insertResume.setLong(1, 60001L);
                assertThat(insertResume.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement updateResume = connection.prepareStatement("""
                    UPDATE biz_resume
                    SET status = 1, version = version + 1
                    WHERE user_id = ? AND status = 0 AND version = 0 AND deleted = 0
                    """)) {
                updateResume.setLong(1, 60001L);
                assertThat(updateResume.executeUpdate()).isEqualTo(1);
                assertThat(updateResume.executeUpdate()).isZero();
            }

            try (PreparedStatement insertVote = connection.prepareStatement("""
                    INSERT INTO sys_vote_record (proposal_id, voter_id, result, weight)
                    VALUES (70001, 80001, 1, 1)
                    """)) {
                assertThat(insertVote.executeUpdate()).isEqualTo(1);
                org.assertj.core.api.Assertions.assertThatThrownBy(insertVote::executeUpdate)
                        .isInstanceOf(java.sql.SQLException.class)
                        .hasMessageContaining("Duplicate");
            }
        }

        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfiguration);
        connectionFactory.afterPropertiesSet();
        try {
            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();
            assertThat(template.opsForValue().setIfAbsent(
                    "csa:integration:lock", "first", Duration.ofSeconds(30))).isTrue();
            assertThat(template.opsForValue().setIfAbsent(
                    "csa:integration:lock", "second", Duration.ofSeconds(30))).isFalse();
            assertThat(template.opsForValue().get("csa:integration:lock")).isEqualTo("first");
        } finally {
            connectionFactory.destroy();
        }
    }
}
