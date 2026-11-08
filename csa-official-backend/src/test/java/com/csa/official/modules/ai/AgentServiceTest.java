package com.csa.official.modules.ai;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.service.CompetitionService;
import com.csa.official.modules.sys.service.ResourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.csa.official.modules.ai.KnowledgeServiceTest.user;

class AgentServiceTest {
    JdbcTemplate db; AiProperties config; KnowledgeService knowledge; ModelGateway gateway; AgentService agent;
    ObjectMapper json=new ObjectMapper();
    @BeforeEach void setup() {
        db=KnowledgeServiceTest.database(); config=new AiProperties(); gateway=mock(ModelGateway.class);
        knowledge=new KnowledgeService(db,gateway,config,json);
        agent=new AgentService(knowledge,mock(ResourceService.class),mock(CompetitionService.class),gateway,config,db,json,new SimpleMeterRegistry());
    }
    @Test void offlineRunPersistsEvidenceAndOwnerIsolation() {
        knowledge.create(new AiModels.DocumentRequest("Java","Java 使用线程池处理异步任务","",1),user(1,3));
        var result=agent.run("Java 怎么处理异步任务",user(1,1));
        assertThat(result.mode()).isEqualTo("offline");
        assertThat(result.question()).isEqualTo("Java 怎么处理异步任务");
        assertThat(result.answer()).contains("未调用大模型");
        assertThat(result.citations()).hasSize(1);
        assertThat(result.steps()).extracting(AiModels.Step::name).contains("search_knowledge");
        assertThat(agent.get(result.id(),user(1,1))).isEqualTo(result);
        assertThatThrownBy(()->agent.get(result.id(),user(2,1))).isInstanceOf(CsaException.class);
        verifyNoInteractions(gateway);
    }
    @Test void oldRunCannotBypassDowngradeOrDocumentDeletion() {
        var doc=knowledge.create(new AiModels.DocumentRequest("Java","Java 秘密","",3),user(1,3));
        var result=agent.run("Java",user(1,3));
        assertThatThrownBy(()->agent.get(result.id(),user(1,1))).isInstanceOf(CsaException.class);
        knowledge.delete(doc.id(),user(1,3));
        assertThatThrownBy(()->agent.get(result.id(),user(1,3))).isInstanceOf(CsaException.class);
    }
    @Test void modelLoopRejectsUnknownToolsAndStopsAtIterationBudget() throws Exception {
        config.setEnabled(true); config.setModel("fixture"); config.setMaxIterations(2);
        when(gateway.chat(anyList(),anyList())).thenReturn(json.readTree("""
                {"choices":[{"message":{"role":"assistant","tool_calls":[{"id":"call_1","type":"function","function":{"name":"delete_all_users","arguments":"{\\"query\\":\\"Java\\"}"}}]}}],"usage":{"prompt_tokens":10,"completion_tokens":5}}
                """));
        var result=agent.run("Java",user(1,1));
        assertThat(result.steps()).anyMatch(s->s.name().equals("unknown_tool")&&s.status().equals("error"));
        assertThat(result.steps()).anyMatch(s->s.name().equals("budget")&&s.status().equals("stopped"));
        assertThat(result.promptTokens()).isEqualTo(20);
        verify(gateway,times(2)).chat(anyList(),anyList());
    }
    @Test void validToolCallingRoundProducesFinalAnswerAndTrace() throws Exception {
        config.setEnabled(true); config.setModel("fixture");
        knowledge.create(new AiModels.DocumentRequest("Java","Java 线程池","",1),user(1,3));
        when(gateway.chat(anyList(),anyList())).thenReturn(json.readTree("""
                {"choices":[{"message":{"role":"assistant","tool_calls":[{"id":"call_1","type":"function","function":{"name":"search_knowledge","arguments":"{\\"query\\":\\"Java\\"}"}}]}}]}
                """),json.readTree("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"使用线程池。\"}}]}"));
        var result=agent.run("Java",user(1,1));
        assertThat(result.answer()).isEqualTo("使用线程池。");
        assertThat(result.steps().stream().filter(s->s.name().equals("search_knowledge"))).hasSize(2);
        assertThat(result.citations()).hasSize(1);
    }
    @Test void rejectsGuestBeforeAnyRetrieval() {
        assertThatThrownBy(()->agent.run("Java",user(1,0))).isInstanceOf(CsaException.class);
        verifyNoInteractions(gateway);
    }
    @Test void broadBusinessBrowseDoesNotRequireGenericWordInTitle() {
        assertThat(AgentService.matches("最近有哪些比赛", "蓝桥杯 程序设计")).isTrue();
        assertThat(AgentService.matches("推荐 Java 学习资料", "Java 线程池")).isTrue();
        assertThat(AgentService.matches("推荐 Java 学习资料", "Python 教程")).isFalse();
    }
}
