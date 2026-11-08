package com.csa.official.modules.ai;

import jakarta.validation.constraints.*;
import java.util.List;

public final class AiModels {
    private AiModels() {}
    public record Document(String id, String title, String source, int minRoleLevel, int chunkCount) {}
    public record DocumentRequest(@NotBlank @Size(max=200) String title,
            @NotBlank @Size(max=50000) String content, @Size(max=500) String source,
            @Min(1) @Max(99) int minRoleLevel) {}
    public record RunRequest(@NotBlank @Size(max=2000) String question) {}
    public record Citation(String documentId, String chunkId, String title, String source, String text, double score) {}
    public record Step(String name, String status, long durationMs, String detail) {}
    public record RunResult(String id, String answer, String mode, String model,
            List<Citation> citations, List<Step> steps, long durationMs, int promptTokens, int completionTokens,
            String question) {}
    public record Status(String mode, String model, String embeddingMode) {}
}
