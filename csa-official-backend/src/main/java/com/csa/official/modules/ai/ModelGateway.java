package com.csa.official.modules.ai;

import com.csa.official.common.exception.CsaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Small OpenAI-compatible adapter; request destinations are operator configuration only. */
@Service
public class ModelGateway {
    private final AiProperties properties;
    private final ObjectMapper json;
    private final HttpClient client;
    public ModelGateway(AiProperties properties, ObjectMapper json) {
        this.properties=properties; this.json=json;
        this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    public JsonNode chat(List<Map<String,Object>> messages, List<Map<String,Object>> tools) {
        return post("/chat/completions", Map.of("model", properties.getModel(), "messages", messages,
                "tools", tools, "tool_choice", "auto", "temperature", 0, "max_tokens", 1200));
    }
    public List<double[]> embed(List<String> inputs) {
        JsonNode data=post("/embeddings", Map.of("model", properties.getEmbeddingModel(), "input", inputs)).path("data");
        if (!data.isArray() || data.size()!=inputs.size()) throw failure();
        double[][] vectors=new double[inputs.size()][];
        for(JsonNode row:data) {
            int index=row.path("index").asInt(-1);
            JsonNode array=row.path("embedding");
            if(index<0||index>=vectors.length||vectors[index]!=null||!array.isArray()||array.isEmpty()||array.size()>8192) throw failure();
            double[] vector=new double[array.size()];
            for(int i=0;i<vector.length;i++) {
                if(!array.get(i).isNumber()||!Double.isFinite(array.get(i).asDouble())) throw failure();
                vector[i]=array.get(i).asDouble();
            }
            vectors[index]=vector;
        }
        return Arrays.asList(vectors);
    }
    private JsonNode post(String path, Map<String,Object> body) {
        if(!properties.isEnabled()||properties.getApiKey().isBlank()||properties.getModel().isBlank())
            throw new CsaException(503,"AI_PROVIDER_NOT_CONFIGURED","请配置 AI 模型和凭据，或使用离线模式");
        try {
            URI uri=URI.create(properties.getBaseUrl().replaceAll("/+$", "")+path);
            if(!Set.of("https","http").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)
                throw failure();
            HttpRequest request=HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(Math.max(1,Math.min(30,properties.getTimeoutSeconds()))))
                    .header("Content-Type","application/json").header("Authorization","Bearer "+properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            CompletableFuture<HttpResponse<String>> pending=client.sendAsync(request,ignored->new LimitedBody());
            HttpResponse<String> response;
            try {
                // Independent deadline also bounds a server that sends headers then stalls its body.
                response=pending.get(Math.max(1,Math.min(30,properties.getTimeoutSeconds())),TimeUnit.SECONDS);
            } finally {
                if(!pending.isDone()) pending.cancel(true);
            }
            if(response.statusCode()<200||response.statusCode()>=300||response.body().length()>2_000_000) throw failure();
            return json.readTree(response.body());
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt(); throw failure();
        } catch(CsaException e) { throw e; }
        catch(Exception e) { throw failure(); }
    }
    private CsaException failure() { return new CsaException(502,"AI_PROVIDER_ERROR","模型服务暂时不可用或响应格式不正确"); }
    /** Cancels the network subscription before buffering more than 2 MB. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<String> {
        private final CompletableFuture<String> future=new CompletableFuture<>();
        private final ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        public CompletionStage<String> getBody() { return future; }
        public void onSubscribe(Flow.Subscription value) { subscription=value; value.request(1); }
        public void onNext(List<ByteBuffer> items) {
            for(ByteBuffer item:items) {
                if(buffer.size()+item.remaining()>2_000_000) {
                    subscription.cancel(); future.completeExceptionally(new IllegalStateException("Provider response too large")); return;
                }
                byte[] bytes=new byte[item.remaining()]; item.get(bytes); buffer.writeBytes(bytes);
            }
            subscription.request(1);
        }
        public void onError(Throwable cause) { future.completeExceptionally(cause); }
        public void onComplete() { future.complete(buffer.toString(StandardCharsets.UTF_8)); }
    }
}
