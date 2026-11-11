package com.storch.smartjobs.integration.gmail;

import com.sun.net.httpserver.HttpServer;
import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.GmailConnection;
import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GmailOAuthServiceTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void authorizationUrlRequestsOnlyOfflineReadAccessAndPreservesState() {
        GmailOAuthService oauth = oauth(mock(GmailConnectionRepository.class));

        String url = oauth.authorizationUrl("csrf-state");

        assertTrue(url.contains("scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fgmail.readonly"));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("state=csrf-state"));
        assertFalse(url.contains("prompt=consent"));
        assertFalse(url.contains("gmail.modify"));
    }

    @Test
    void exchangesCodeAndStoresUserBoundEncryptedTokens() throws Exception {
        GmailConnectionRepository repository = mock(GmailConnectionRepository.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(41L);
        startServer(exchange -> {
            if (exchange.getRequestURI().getPath().equals("/token")) {
                String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(form.contains("code=authorization-code"));
                assertTrue(form.contains("redirect_uri=http%3A%2F%2Flocalhost%2Fcallback"));
                respond(exchange, 200, "{\"access_token\":\"access-secret\",\"refresh_token\":\"refresh-secret\",\"expires_in\":3600}");
            } else {
                assertEquals("Bearer access-secret", exchange.getRequestHeaders().getFirst("Authorization"));
                respond(exchange, 200, "{\"emailAddress\":\"jobs@example.com\"}");
            }
        });
        when(repository.findByUserId(41L)).thenReturn(Optional.empty());

        oauth(repository).connect(user, "authorization-code");

        var saved = org.mockito.ArgumentCaptor.forClass(GmailConnection.class);
        verify(repository).save(saved.capture());
        GmailConnection connection = saved.getValue();
        assertEquals("jobs@example.com", connection.getConnectedEmail());
        assertNotEquals("access-secret", connection.getAccessTokenCiphertext());
        assertNotEquals("refresh-secret", connection.getRefreshTokenCiphertext());
        GmailTokenCipher cipher = new GmailTokenCipher("0123456789abcdef0123456789abcdef");
        assertEquals("access-secret", cipher.decrypt(connection.getAccessTokenCiphertext(), "41"));
        assertEquals("refresh-secret", cipher.decrypt(connection.getRefreshTokenCiphertext(), "41"));
        assertThrows(IllegalStateException.class,
                () -> cipher.decrypt(connection.getAccessTokenCiphertext(), "42"));
    }

    @Test
    void refreshesExpiredAccessTokenAndRemovesConnectionOnRevokedGrant() throws Exception {
        GmailConnectionRepository repository = mock(GmailConnectionRepository.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(41L);
        GmailTokenCipher cipher = new GmailTokenCipher("0123456789abcdef0123456789abcdef");
        GmailConnection expired = new GmailConnection(user, cipher.encrypt("old-access", "41"),
                cipher.encrypt("refresh-secret", "41"), Instant.now().minusSeconds(1), "jobs@example.com");
        when(repository.findByUserId(41L)).thenReturn(Optional.of(expired));
        startServer(exchange -> {
            String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(form.contains("refresh_token=refresh-secret"));
            respond(exchange, 400, "{\"error\":\"invalid_grant\"}");
        });

        GmailOAuthService oauth = oauth(repository);
        GmailAuthorizationException failure = assertThrows(GmailAuthorizationException.class, () -> oauth.accessToken(user));

        assertTrue(failure.getMessage().contains("revogada"));
        verify(repository).deleteByUserId(41L);
    }

    @Test
    void disconnectDeletesLocalTokensAndAttemptsGoogleRevocation() throws Exception {
        GmailConnectionRepository repository = mock(GmailConnectionRepository.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(41L);
        GmailTokenCipher cipher = new GmailTokenCipher("0123456789abcdef0123456789abcdef");
        GmailConnection connection = new GmailConnection(user, cipher.encrypt("access", "41"),
                cipher.encrypt("refresh-secret", "41"), Instant.now().plusSeconds(3600), "jobs@example.com");
        when(repository.findByUserId(41L)).thenReturn(Optional.of(connection));
        startServer(exchange -> {
            assertEquals("/revoke", exchange.getRequestURI().getPath());
            String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(form.contains("token=refresh-secret"));
            respond(exchange, 200, "");
        });

        assertTrue(oauth(repository).disconnect(user));

        verify(repository).delete(connection);
    }

    private GmailOAuthService oauth(GmailConnectionRepository repository) {
        String root = server == null ? "http://127.0.0.1" : "http://127.0.0.1:" + server.getAddress().getPort();
        return new GmailOAuthService("client-id", "client-secret", "http://localhost/callback",
                "0123456789abcdef0123456789abcdef", 5, root + "/token", root + "/revoke",
                root + "/profile", repository, RestClient.builder());
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
