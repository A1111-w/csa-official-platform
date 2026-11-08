package com.csa.official.modules.ai;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class EvaluationMetricsTest {
    @Test void computesRanksAndRecallWithoutCountingDuplicateChunksOrLabels() {
        var expected = new EvaluationModels.Case("RAG", List.of("a", "b", "a"),
                List.of("search_knowledge", "search_resources"), List.of("RAG", "Java"));
        var scores = EvaluationMetrics.score(expected, List.of("noise", "a", "a"),
                Set.of("search_knowledge"), "rag uses retrieval");
        assertThat(scores.recall()).isEqualTo(.5);
        assertThat(scores.reciprocalRank()).isEqualTo(.5);
        assertThat(scores.toolRecall()).isEqualTo(.5);
        assertThat(scores.termCoverage()).isEqualTo(.5);
        assertThat(scores.passed()).isFalse();
    }

    @Test void missingLabelsAreNotPerfectScoresAndUnannotatedCaseCannotPass() {
        var expected = new EvaluationModels.Case("hello", List.of(), List.of(), List.of());
        var scores = EvaluationMetrics.score(expected, List.of(), Set.of(), "hello");
        assertThat(scores.recall()).isNull();
        assertThat(scores.reciprocalRank()).isNull();
        assertThat(scores.toolRecall()).isNull();
        assertThat(scores.termCoverage()).isNull();
        assertThat(scores.passed()).isFalse();
    }

    @Test void emptyRetrievalScoresZeroAndAllExpectedChecksAreRequired() {
        var expected = new EvaluationModels.Case("hello", List.of("a"), List.of(), List.of("JAVA"));
        var missing = EvaluationMetrics.score(expected, List.of(), Set.of(), "Java");
        assertThat(missing.recall()).isZero();
        assertThat(missing.reciprocalRank()).isZero();
        assertThat(missing.passed()).isFalse();
        assertThat(EvaluationMetrics.score(expected, List.of("a"), Set.of(), "Java").passed()).isTrue();
    }

    @Test void rejectsEmptyOversizedAndUnannotatedDatasets() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new EvaluationModels.Request(List.of()))).isNotEmpty();
            var unlabeled = new EvaluationModels.Case("hello", List.of(), List.of(), List.of());
            assertThat(validator.validate(new EvaluationModels.Request(List.of(unlabeled)))).isNotEmpty();
            var valid = new EvaluationModels.Case("RAG", List.of(), List.of("search_knowledge"), List.of());
            assertThat(validator.validate(new EvaluationModels.Request(List.of(valid)))).isEmpty();
            assertThat(validator.validate(new EvaluationModels.Request(
                    List.of(valid, valid, valid, valid, valid, valid)))).isNotEmpty();
        }
    }
}
