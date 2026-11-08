package com.csa.official.modules.ai;

import com.csa.official.common.exception.CsaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class ModelGatewayTest {
    HttpServer server; AiProperties config; ModelGateway gateway;
    AtomicReference<String> response=new AtomicReference<>("{}");
    AtomicReference<String> body=new AtomicReference<>();
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1", exchange->{
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes=response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);
            try(var out=exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        config=new AiProperties(); config.setEnabled(true); config.setApiKey("test-only"); config.setModel("fixture");
        config.setEmbeddingModel("fixture-embedding"); config.setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/v1");
        gateway=new ModelGateway(config,new ObjectMapper());
    }
    @AfterEach void stop() { server.stop(0); }
    @Test void serializesCompatibleChatRequestAndDecodesUsage() {
        response.set("{\"choices\":[{\"message\":{\"content\":\"hello\"}}],\"usage\":{\"prompt_tokens\":3}}");
        var result=gateway.chat(List.of(Map.of("role","user","content","hi")),List.of());
        assertThat(result.path("usage").path("prompt_tokens").asInt()).isEqualTo(3);
        assertThat(body.get()).contains("\"model\":\"fixture\"","\"max_tokens\":1200");
    }
    @Test void restoresEmbeddingInputOrderUsingIndex() {
        response.set("{\"data\":[{\"index\":1,\"embedding\":[0,1]},{\"index\":0,\"embedding\":[1,0]}]}");
        assertThat(gateway.embed(List.of("a","b")).get(0)).containsExactly(1,0);
    }
    @Test void rejectsDuplicateEmbeddingIndices() {
        response.set("{\"data\":[{\"index\":0,\"embedding\":[0,1]},{\"index\":0,\"embedding\":[1,0]}]}");
        assertThatThrownBy(()->gateway.embed(List.of("a","b"))).isInstanceOf(CsaException.class);
    }
    @Test void responseSizeIsBoundedAndErrorsHideProviderBody() {
        response.set("sensitive-provider-content"+"x".repeat(2_000_000));
        assertThatThrownBy(()->gateway.chat(List.of(),List.of())).isInstanceOf(CsaException.class)
                .hasMessageNotContaining("sensitive-provider-content");
    }
    @Test void timesOutWhenProviderStallsAfterHeaders() throws Exception {
        server.createContext("/slow/chat/completions", exchange -> {
            exchange.sendResponseHeaders(200, 100);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/slow");
        config.setTimeoutSeconds(1);
        long started = System.nanoTime();
        assertThatThrownBy(() -> gateway.chat(List.of(), List.of()))
                .isInstanceOf(CsaException.class).hasMessageNotContaining("TimeoutException");
        assertThat((System.nanoTime() - started) / 1_000_000).isLessThan(3000);
    }
    @Test void offlineNeverMakesNetworkRequest() {
        config.setEnabled(false);
        assertThatThrownBy(()->gateway.chat(List.of(),List.of())).isInstanceOf(CsaException.class);
        assertThat(body.get()).isNull();
    }
}
