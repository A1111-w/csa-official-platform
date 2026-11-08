package com.csa.official.modules.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="csa.ai")
public class AiProperties {
    private boolean enabled = false;
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "";
    private String embeddingModel = "";
    private int maxIterations = 4;
    private int timeoutSeconds = 25;
    public String mode() { return enabled ? "live" : "offline"; }
    public String displayModel() { return enabled ? model : "extractive-bm25"; }
    public boolean embeddingsEnabled() { return enabled && !embeddingModel.isBlank(); }
}
