package com.csa.official.modules.ai;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeServiceTest {
    JdbcTemplate db; KnowledgeService service; AiProperties config; ModelGateway gateway;
    static JdbcTemplate database() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:ai-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V7__ai_knowledge_and_runs.sql")).execute(source);
        return new JdbcTemplate(source);
    }
    static User user(long id,int role) { var user=new User(); user.setId(id); user.setRoleLevel(role); return user; }
    @BeforeEach void setup() { db=database(); config=new AiProperties(); gateway=mock(ModelGateway.class); service=new KnowledgeService(db,gateway,config,new ObjectMapper()); }
    @Test void migrationAndAclExcludePrivateChunksBeforeTopK() {
        var visible=service.create(new AiModels.DocumentRequest("公开教程","Java 并发编程 CompletableFuture 学习资料","manual",1),user(1,3));
        service.create(new AiModels.DocumentRequest("机密 Java Java Java","Java Java Java 并发编程，部长内部资料","secret",3),user(1,3));
        assertThat(service.list(1)).extracting(AiModels.Document::id).containsExactly(visible.id());
        assertThat(service.search("Java 并发编程",1,1)).extracting(AiModels.Citation::documentId).containsExactly(visible.id());
        assertThat(service.search("机密",1,5)).isEmpty();
        verifyNoInteractions(gateway);
    }
    @Test void handlesChineseAndOverlappingChunks() {
        String content="知识库检索".repeat(250);
        var chunks=KnowledgeService.split(content);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).substring(600)).isEqualTo(chunks.get(1).substring(0,100));
        assertThat(KnowledgeService.tokens("Java 知识库检索")).contains("java","知识","识库","库检","检索");
    }
    @Test void preventsInternalMutationByLowRoleAndHigherAclAssignment() {
        var request=new AiModels.DocumentRequest("x","text","",1);
        assertThatThrownBy(()->service.create(request,user(1,1))).isInstanceOf(CsaException.class);
        assertThatThrownBy(()->service.create(new AiModels.DocumentRequest("x","text","",4),user(1,3))).isInstanceOf(CsaException.class);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM ai_document",Integer.class)).isZero();
    }
    @Test void semanticRetrievalCanFindNoLexicalOverlapAndUsesModelIdentity() {
        config.setEnabled(true); config.setEmbeddingModel("fixture-embedding");
        when(gateway.embed(List.of("异步任务执行"))).thenReturn(List.of(new double[]{1,0}));
        var doc=service.create(new AiModels.DocumentRequest("指南","异步任务执行","",1),user(1,3));
        when(gateway.embed(List.of("parallel work"))).thenReturn(List.of(new double[]{1,0}));
        assertThat(service.search("parallel work",1,3)).extracting(AiModels.Citation::documentId).containsExactly(doc.id());
        config.setEmbeddingModel("different-model");
        assertThat(service.search("parallel work",1,3)).isEmpty();
    }
    @Test void deletionCleansChunksAndRevokesRead() {
        var doc=service.create(new AiModels.DocumentRequest("Java","Java 线程池","",1),user(1,3));
        service.delete(doc.id(),user(1,3));
        assertThat(service.canRead(doc.id(),1)).isFalse();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM ai_chunk",Integer.class)).isZero();
    }
}
