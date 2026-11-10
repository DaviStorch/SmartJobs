package com.storch.smartjobs.integration.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatibleProviderTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsExistingScoreAndReadsCompatibleChatCompletionResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(request.contains("\"score\"") || request.contains("4"));
            byte[] response = """
                    {"choices":[{"message":{"content":"Explique os requisitos atendidos."}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        var provider = new OpenAiCompatibleProvider(true,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions",
                "test-model", "", 5);

        String answer = provider.explain(new AiProvider.JobAdviceContext(
                "Developer", "Example", "Build APIs", "4", "pt", List.of("Java"), List.of("Kubernetes"), "Rationale"));

        assertEquals("Explique os requisitos atendidos.", answer);
    }

    @Test
    void reportsProviderHttpErrorsInsteadOfReturningAnEmptyAnswer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        var provider = new OpenAiCompatibleProvider(true,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/",
                "test-model", "", 5);

        AiUnavailableException failure = assertThrows(AiUnavailableException.class,
                () -> provider.explain(new AiProvider.JobAdviceContext("Title", "Company", "", "2", "en", List.of(), List.of(), "")));
        assertTrue(failure.getMessage().contains("HTTP 503"));
    }

    @Test
    void rejectsUnencryptedRemoteEndpoints() {
        var provider = new OpenAiCompatibleProvider(true, "http://example.org/chat", "model", "", 5);
        assertThrows(AiUnavailableException.class,
                () -> provider.explain(new AiProvider.JobAdviceContext("Title", "Company", "", "2", "en", List.of(), List.of(), "")));
    }
}
