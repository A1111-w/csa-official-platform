package com.csa.official.modules.ai;

import java.util.*;

/** Deterministic evidence/tool/keyword checks, deliberately not an LLM faithfulness judge. */
public final class EvaluationMetrics {
    private EvaluationMetrics() {}

    public record Scores(Double recall, Double reciprocalRank, Double toolRecall, Double termCoverage) {
        public boolean passed() {
            return (recall != null || toolRecall != null || termCoverage != null)
                    && (recall == null || recall == 1d)
                    && (toolRecall == null || toolRecall == 1d)
                    && (termCoverage == null || termCoverage == 1d);
        }
    }

    public static Scores score(EvaluationModels.Case expected, List<String> documentIds,
                               Set<String> successfulTools, String answer) {
        List<String> ranked = documentIds.stream().distinct().toList();
        Set<String> relevant = new HashSet<>(expected.expectedDocumentIds());
        Double recall = coverage(relevant, new HashSet<>(ranked));
        Double reciprocalRank = relevant.isEmpty() ? null : 0d;
        for (int i = 0; i < ranked.size(); i++) {
            if (relevant.contains(ranked.get(i))) {
                reciprocalRank = 1d / (i + 1);
                break;
            }
        }
        Double toolRecall = coverage(new HashSet<>(expected.expectedTools()), successfulTools);
        Set<String> terms = new HashSet<>();
        for (String term : expected.expectedAnswerTerms()) terms.add(term.toLowerCase(Locale.ROOT));
        Set<String> hits = new HashSet<>();
        String normalized = answer == null ? "" : answer.toLowerCase(Locale.ROOT);
        for (String term : terms) if (normalized.contains(term)) hits.add(term);
        return new Scores(recall, reciprocalRank, toolRecall, coverage(terms, hits));
    }

    private static Double coverage(Set<String> expected, Set<String> actual) {
        if (expected.isEmpty()) return null;
        return (double) expected.stream().filter(actual::contains).count() / expected.size();
    }
}
