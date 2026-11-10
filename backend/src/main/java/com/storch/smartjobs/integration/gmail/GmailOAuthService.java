package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.GmailConnection;
import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class GmailOAuthService {
    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";
    private static final String GOOGLE_PROFILE_ENDPOINT = "https://gmail.googleapis.com/gmail/v1/users/me/profile";
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String encryptionSecret;
    private final GmailConnectionRepository connections;
    private final RestClient http;

    public GmailOAuthService(
            @Value("${smartjobs.gmail.client-id:}") String clientId,
            @Value("${smartjobs.gmail.client-secret:}") String clientSecret,
            @Value("${smartjobs.gmail.redirect-uri:}") String redirectUri,
            @Value("${smartjobs.gmail.encryption-key:}") String encryptionSecret,
            GmailConnectionRepository connections, RestClient.Builder builder) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.encryptionSecret = encryptionSecret;
        this.connections = connections;
        this.http = builder.build();
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
                + "&access_type=offline&prompt=consent&include_granted_scopes=true&state=" + enc(state);
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
        String email = string(apiGet(GOOGLE_PROFILE_ENDPOINT, accessToken).get("emailAddress"));
        GmailTokenCipher cipher = cipher();
        String context = user.getId().toString();
        connections.save(new GmailConnection(user, cipher.encrypt(accessToken, context), cipher.encrypt(refreshToken, context),
                Instant.now().plusSeconds(number(token.get("expires_in"), 3600)), email));
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
        Map<?, ?> response = tokenRequest(Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "refresh_token", refreshToken,
                "grant_type", "refresh_token"));
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
            http.post().uri(GOOGLE_REVOKE_ENDPOINT).contentType(MediaType.APPLICATION_FORM_URLENCODED)
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
        try {
            return http.get().uri(uri).header("Authorization", "Bearer " + accessToken)
                    .retrieve().body(Map.class);
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível acessar o Gmail. Verifique a conexão ou reconecte a conta se a permissão tiver sido revogada.", exception);
        }
    }

    public void markSynced(AppUser user, Instant time) {
        connections.findByUserId(user.getId()).ifPresent(connection -> {
            connection.setLastSyncedAt(time);
            connections.save(connection);
        });
    }

    private Map<?, ?> tokenRequest(Map<String, String> values) {
        try {
            return http.post().uri(GOOGLE_TOKEN_ENDPOINT).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form(values)).retrieve().body(Map.class);
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
