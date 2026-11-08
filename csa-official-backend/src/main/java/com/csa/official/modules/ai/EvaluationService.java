package com.csa.official.modules.ai;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EvaluationService {
    private final AgentService agent;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Validator validator;

    public EvaluationService(AgentService agent, JdbcTemplate jdbc, ObjectMapper json, Validator validator) {
        this.agent = agent;
        this.jdbc = jdbc;
        this.json = json;
        this.validator = validator;
    }

    public EvaluationModels.Report evaluate(EvaluationModels.Request request, User user) {
        requireManager(user);
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new CsaException(400, "评测需要 1–5 个有效的标注用例");
        }
        List<EvaluationModels.Result> results = new ArrayList<>();
        Set<String> modes = new HashSet<>();
        for (var example : request.cases()) {
            long start = System.nanoTime();
            try {
                var run = agent.run(example.question(), user);
                modes.add(run.mode());
                var tools = run.steps().stream()
                        .filter(step -> "success".equalsIgnoreCase(step.status()))
                        .map(AiModels.Step::name).collect(Collectors.toSet());
                var scores = EvaluationMetrics.score(example,
                        run.citations().stream().map(AiModels.Citation::documentId).toList(), tools, run.answer());
                boolean stopped = run.steps().stream().anyMatch(step -> "stopped".equalsIgnoreCase(step.status()));
                results.add(new EvaluationModels.Result(example.question(), run.id(), scores.passed() && !stopped,
                        scores.recall(), scores.reciprocalRank(), scores.toolRecall(), scores.termCoverage(),
                        elapsed(start), stopped ? "执行预算耗尽，未生成最终回答" : null));
            } catch (RuntimeException e) {
                // Provider bodies, SQL details and credentials must never become a report field.
                String error = e instanceof CsaException ce && ce.getCode() == 429
                        ? "请求受限，请稍后重试" : "执行失败，请检查服务配置或稍后重试";
                results.add(new EvaluationModels.Result(example.question(), null, false,
                        example.expectedDocumentIds().isEmpty() ? null : 0d,
                        example.expectedDocumentIds().isEmpty() ? null : 0d,
                        example.expectedTools().isEmpty() ? null : 0d,
                        example.expectedAnswerTerms().isEmpty() ? null : 0d,
                        elapsed(start), error));
            }
        }
        int passed = (int) results.stream().filter(EvaluationModels.Result::passed).count();
        var report = new EvaluationModels.Report(UUID.randomUUID().toString(),
                modes.isEmpty() ? "unknown" : modes.size() == 1 ? modes.iterator().next() : "mixed",
                results.size(), passed, (double) passed / results.size(),
                mean(results, EvaluationModels.Result::reciprocalRank),
                mean(results, EvaluationModels.Result::recallAtK),
                mean(results, EvaluationModels.Result::toolRecall),
                mean(results, EvaluationModels.Result::answerTermCoverage),
                results.stream().mapToLong(EvaluationModels.Result::latencyMs).average().orElse(0),
                List.copyOf(results), "rules-v1", List.copyOf(request.cases()));
        try {
            jdbc.update("INSERT INTO ai_evaluation (id, owner_id, report_json) VALUES (?, ?, ?)",
                    report.id(), user.getId(), json.writeValueAsString(report));
        } catch (JsonProcessingException e) {
            throw new CsaException(500, "评测报告序列化失败");
        }
        return report;
    }

    public EvaluationModels.Report get(String id, User user) {
        requireManager(user);
        List<String> reports = jdbc.query("SELECT report_json FROM ai_evaluation WHERE id = ? AND owner_id = ?",
                (rs, row) -> rs.getString(1), id, user.getId());
        if (reports.isEmpty()) throw new CsaException(404, "评测报告不存在");
        try {
            return json.readValue(reports.get(0), EvaluationModels.Report.class);
        } catch (JsonProcessingException e) {
            throw new CsaException(500, "评测报告读取失败");
        }
    }

    private static void requireManager(User user) {
        if (user == null || user.getId() == null || user.getRoleLevel() == null || user.getRoleLevel() < 3)
            throw new CsaException(403, "需要部长及以上权限");
    }

    private static long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }

    private static Double mean(List<EvaluationModels.Result> rows, Function<EvaluationModels.Result, Double> metric) {
        var average = rows.stream().map(metric).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average();
        return average.isPresent() ? average.getAsDouble() : null;
    }
}
