package com.csa.official.modules.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public final class EvaluationModels {
    private EvaluationModels() {}

    public record Case(
            @NotBlank @Size(max = 2000) String question,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> expectedDocumentIds,
            @NotNull @Size(max = 3) List<@NotBlank @Pattern(regexp = "search_knowledge|search_resources|search_competitions") String> expectedTools,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 200) String> expectedAnswerTerms) {
        @AssertTrue(message = "每个用例至少填写一项预期文档、工具或答案关键词")
        @JsonIgnore
        public boolean isAnnotated() {
            return (expectedDocumentIds != null && !expectedDocumentIds.isEmpty())
                    || (expectedTools != null && !expectedTools.isEmpty())
                    || (expectedAnswerTerms != null && !expectedAnswerTerms.isEmpty());
        }
    }

    public record Request(@NotNull @Size(min = 1, max = 5) List<@NotNull @Valid Case> cases) {}

    public record Result(String question, String runId, boolean passed, Double recallAtK,
                         Double reciprocalRank, Double toolRecall, Double answerTermCoverage,
                         long latencyMs, String error) {}

    public record Report(String id, String mode, int total, int passed, double passRate,
                         Double meanReciprocalRank, Double meanRecallAtK, Double meanToolRecall,
                         Double meanAnswerTermCoverage, double averageLatencyMs,
                         List<Result> results, String evaluatorVersion, List<Case> dataset) {}
}
