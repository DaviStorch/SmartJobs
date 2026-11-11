package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.GmailConnection;
import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class GmailOAuthService {
    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final String GOOGLE_PROFILE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/profile";
    private final String tokenEndpoint;
    private final String revokeEndpoint;
    private final String profileEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String encryptionSecret;
    private final GmailConnectionRepository connections;
    private final RestClient http;

    @Autowired
    public GmailOAuthService(
            @Value("${smartjobs.gmail.client-id:}") String clientId,
            @Value("${smartjobs.gmail.client-secret:}") String clientSecret,
            @Value("${smartjobs.gmail.redirect-uri:}") String redirectUri,
            @Value("${smartjobs.gmail.encryption-key:}") String encryptionSecret,
            @Value("${smartjobs.gmail.timeout-seconds:15}") int timeoutSeconds,
            GmailConnectionRepository connections, RestClient.Builder builder) {
        this(clientId, clientSecret, redirectUri, encryptionSecret, timeoutSeconds,
                GOOGLE_TOKEN_ENDPOINT, GOOGLE_REVOKE_ENDPOINT, GOOGLE_PROFILE_ENDPOINT, connections, builder);
    }

    GmailOAuthService(String clientId, String clientSecret, String redirectUri, String encryptionSecret,
                      int timeoutSeconds, String tokenEndpoint, String revokeEndpoint, String profileEndpoint,
                      GmailConnectionRepository connections, RestClient.Builder builder) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.encryptionSecret = encryptionSecret;
        this.tokenEndpoint = tokenEndpoint;
        this.revokeEndpoint = revokeEndpoint;
        this.profileEndpoint = profileEndpoint;
        this.connections = connections;
        int boundedTimeout = Math.max(2, Math.min(timeoutSeconds, 60));
        var client = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(boundedTimeout)).build();
        var requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofSeconds(boundedTimeout));
        this.http = builder.requestFactory(requestFactory).build();
    }

    public boolean configured() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !redirectUri.isBlank()
                && encryptionSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32;
    }

    public String authorizationUrl(String state) {
        if (!configured()) throw new IllegalStateException("Configure as credenciais OAuth e a chave de criptografia do Gmail.");
        return "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code&scope=" + enc("https://www.googleapis.com/auth/gmail.readonly")
                + "&access_type=offline&include_granted_scopes=true&state=" + enc(state);
    }

    public void connect(AppUser user, String code) {
        requireConfigured();
        Map<?, ?> token = tokenRequest(Map.of(
                "code", code,
                "client_id", clientId,
                "client_secret", clientSecret,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"));
        String accessToken = required(token, "access_token");
        String refreshToken = string(token.get("refresh_token"));
        Optional<GmailConnection> existing = connections.findByUserId(user.getId());
        if (refreshToken.isBlank() && existing.isPresent()) {
            refreshToken = decrypt(existing.get().getRefreshTokenCiphertext(), existing.get().getUserId());
        }
        if (refreshToken.isBlank()) throw new GmailIntegrationException("Google não retornou um token de renovação. Desconecte o app Google e tente conectar novamente.");
        String email = string(apiGet(profileEndpoint, accessToken).get("emailAddress"));
        GmailTokenCipher cipher = cipher();
        String context = user.getId().toString();
        GmailConnection connection = new GmailConnection(user, cipher.encrypt(accessToken, context),
                cipher.encrypt(refreshToken, context), Instant.now().plusSeconds(number(token.get("expires_in"), 3600)), email);
        connections.save(connection);
    }

    public String accessToken(AppUser user) {
        requireConfigured();
        GmailConnection connection = connections.findByUserId(user.getId())
                .orElseThrow(() -> new GmailIntegrationException("Conecte uma conta Gmail antes de sincronizar."));
        if (connection.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return cipher().decrypt(connection.getAccessTokenCiphertext(), user.getId().toString());
        }
        String refreshToken = decrypt(connection.getRefreshTokenCiphertext(), user.getId());
        if (refreshToken.isBlank()) throw new GmailIntegrationException("A autorização Gmail expirou. Desconecte e conecte novamente.");
        Map<?, ?> response;
        try {
            response = tokenRequest(Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "refresh_token", refreshToken,
                    "grant_type", "refresh_token"));
        } catch (GmailAuthorizationException exception) {
            connections.deleteByUserId(user.getId());
            throw exception;
        }
        String accessToken = required(response, "access_token");
        connection.setAccessTokenCiphertext(cipher().encrypt(accessToken, user.getId().toString()));
        connection.setExpiresAt(Instant.now().plusSeconds(number(response.get("expires_in"), 3600)));
        connections.save(connection);
        return accessToken;
    }

    public boolean disconnect(AppUser user) {
        GmailConnection connection = connections.findByUserId(user.getId()).orElse(null);
        if (connection == null) return true;
        connections.delete(connection);
        if (!configured()) return false;
        try {
            String token = decrypt(connection.getRefreshTokenCiphertext(), user.getId());
            if (token.isBlank()) return false;
            http.post().uri(revokeEndpoint).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form(Map.of("token", token))).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException | IllegalStateException exception) {
            return false;
        }
    }

    public Optional<GmailConnection> connection(AppUser user) {
        return connections.findByUserId(user.getId());
    }

    public Map<?, ?> apiGet(String uri, String accessToken) {
        return apiGet(uri, accessToken, null);
    }

    public Map<?, ?> apiGet(String uri, String accessToken, AppUser user) {
        try {
            return http.get().uri(uri).header("Authorization", "Bearer " + accessToken)
                    .retrieve().body(Map.class);
        } catch (HttpClientErrorException.Unauthorized exception) {
            if (user != null) connections.deleteByUserId(user.getId());
            throw new GmailAuthorizationException("A autorização Gmail expirou ou foi revogada. Conecte a conta novamente.", exception);
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível acessar o Gmail. Verifique a conexão ou reconecte a conta se a permissão tiver sido revogada.", exception);
        }
    }

    public void markSyncStarted(AppUser user, Instant startedAt) {
        connections.findByUserId(user.getId()).ifPresent(connection -> {
            connection.setSyncStatus("RUNNING");
            connection.setSyncStartedAt(startedAt);
            connection.setLastSyncError(null);
            connections.save(connection);
        });
    }

    public void markSynced(AppUser user, Instant time, GmailSyncResult result) {
        connections.findByUserId(user.getId()).ifPresent(connection -> {
            connection.completeSync(time, result.scanned(), result.receivedCount(), result.imported(),
                    result.duplicates(), result.ignored());
            connections.save(connection);
        });
    }

    public void markSyncFailed(AppUser user, String message) {
        connections.findByUserId(user.getId()).ifPresent(connection -> {
            String safeMessage = message == null ? "Falha na sincronização." : message;
            connection.failSync(safeMessage.substring(0, Math.min(safeMessage.length(), 500)));
            connections.save(connection);
        });
    }

    private Map<?, ?> tokenRequest(Map<String, String> values) {
        try {
            return http.post().uri(tokenEndpoint).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form(values)).retrieve().body(Map.class);
        } catch (RestClientResponseException exception) {
            if (exception.getResponseBodyAsString().contains("invalid_grant")) {
                throw new GmailAuthorizationException("A autorização Gmail expirou ou foi revogada. Conecte a conta novamente.", exception);
            }
            throw new GmailIntegrationException("Não foi possível concluir a autenticação Google. Confira as credenciais e tente novamente.", exception);
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível concluir a autenticação Google. Confira as credenciais e tente novamente.", exception);
        }
    }

    private LinkedMultiValueMap<String, String> form(Map<String, String> values) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        values.forEach(body::add);
        return body;
    }

    private GmailTokenCipher cipher() {
        return new GmailTokenCipher(encryptionSecret);
    }

    private String decrypt(String value, Long userId) {
        return value == null || value.isBlank() ? "" : cipher().decrypt(value, userId.toString());
    }

    private void requireConfigured() {
        if (!configured()) throw new GmailIntegrationException("A integração Gmail não está configurada.");
    }

    private String required(Map<?, ?> values, String key) {
        String value = string(values.get(key));
        if (value.isBlank()) throw new GmailIntegrationException("A resposta do Google não contém o token esperado.");
        return value;
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private long number(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private String enc(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
