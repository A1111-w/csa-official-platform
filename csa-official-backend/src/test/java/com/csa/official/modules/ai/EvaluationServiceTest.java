package com.csa.official.modules.ai;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvaluationServiceTest {
    private AgentService agent;
    private EvaluationService service;
    private ValidatorFactory validation;
    private User owner;

    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:eval-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V8__ai_evaluation_reports.sql"))
                .execute(source);
        agent = mock(AgentService.class);
        validation = Validation.buildDefaultValidatorFactory();
        service = new EvaluationService(agent, new JdbcTemplate(source), new ObjectMapper(), validation.getValidator());
        owner = user(1L, 3);
    }

    @AfterEach void close() { validation.close(); }

    @Test void persistsDatasetAndReportWithOwnerIsolation() {
        var example = new EvaluationModels.Case("RAG", List.of("doc"), List.of("search_knowledge"), List.of("RAG"));
        when(agent.run("RAG", owner)).thenReturn(new AiModels.RunResult("run", "RAG answer", "offline", "extractive",
                List.of(new AiModels.Citation("doc", "chunk", "RAG", "notes", "RAG answer", 1)),
                List.of(new AiModels.Step("search_knowledge", "success", 1, "1 hit")), 2, 0, 0, "RAG"));
        var report = service.evaluate(new EvaluationModels.Request(List.of(example)), owner);
        assertThat(report.passRate()).isEqualTo(1);
        assertThat(report.dataset()).containsExactly(example);
        assertThat(service.get(report.id(), owner)).isEqualTo(report);
        assertThatThrownBy(() -> service.get(report.id(), user(2L, 3)))
                .isInstanceOf(CsaException.class).extracting("code").isEqualTo(404);
    }

    @Test void isolatesProviderFailureAndDoesNotExposeUpstreamSecrets() {
        when(agent.run("first", owner)).thenThrow(new IllegalStateException("secret-provider-body"));
        when(agent.run("second", owner)).thenReturn(new AiModels.RunResult("second-run", "ok", "offline", "extractive",
                List.of(), List.of(new AiModels.Step("search_knowledge", "success", 1, "0 hits")), 1, 0, 0, "second"));
        var report = service.evaluate(new EvaluationModels.Request(List.of(
                new EvaluationModels.Case("first", List.of("a"), List.of(), List.of()),
                new EvaluationModels.Case("second", List.of(), List.of("search_knowledge"), List.of()))), owner);
        assertThat(report.total()).isEqualTo(2);
        assertThat(report.passed()).isEqualTo(1);
        assertThat(report.meanRecallAtK()).isZero();
        assertThat(report.meanToolRecall()).isEqualTo(1);
        assertThat(report.results().get(0).error()).doesNotContain("secret-provider-body");
        assertThat(report.results().get(0).runId()).isNull();
    }

    @Test void rejectedRoleOrDatasetNeverCallsAgent() {
        var request = new EvaluationModels.Request(List.of());
        assertThatThrownBy(() -> service.evaluate(request, user(2L, 1)))
                .isInstanceOf(CsaException.class).extracting("code").isEqualTo(403);
        assertThatThrownBy(() -> service.evaluate(request, owner))
                .isInstanceOf(CsaException.class).extracting("code").isEqualTo(400);
        verifyNoInteractions(agent);
    }

    @Test void exhaustedBudgetCannotPassEvenWhenExpectedToolRan() {
        when(agent.run("question", owner)).thenReturn(new AiModels.RunResult("stopped-run", "预算耗尽", "live", "model",
                List.of(), List.of(new AiModels.Step("search_knowledge", "success", 1, "0 hits"),
                new AiModels.Step("budget", "stopped", 0, "budget exhausted")), 1, 0, 0, "question"));
        var report = service.evaluate(new EvaluationModels.Request(List.of(
                new EvaluationModels.Case("question", List.of(), List.of("search_knowledge"), List.of()))), owner);
        assertThat(report.meanToolRecall()).isEqualTo(1);
        assertThat(report.passed()).isZero();
        assertThat(report.results().get(0).error()).isNotNull();
    }

    private static User user(long id, int role) {
        var user = new User(); user.setId(id); user.setRoleLevel(role); return user;
    }
}
