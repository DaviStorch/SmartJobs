package com.storch.smartjobs.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gmail_connection")
public class GmailConnection {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "access_token_ciphertext", nullable = false, columnDefinition = "text")
    private String accessTokenCiphertext;

    @Column(name = "refresh_token_ciphertext", columnDefinition = "text")
    private String refreshTokenCiphertext;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "connected_email", length = 254)
    private String connectedEmail;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected GmailConnection() {
    }

    public GmailConnection(AppUser user, String accessTokenCiphertext, String refreshTokenCiphertext,
                           Instant expiresAt, String connectedEmail) {
        this.user = user;
        this.accessTokenCiphertext = accessTokenCiphertext;
        this.refreshTokenCiphertext = refreshTokenCiphertext;
        this.expiresAt = expiresAt;
        this.connectedEmail = connectedEmail;
    }

    public Long getUserId() { return userId; }
    public String getAccessTokenCiphertext() { return accessTokenCiphertext; }
    public void setAccessTokenCiphertext(String value) { this.accessTokenCiphertext = value; }
    public String getRefreshTokenCiphertext() { return refreshTokenCiphertext; }
    public void setRefreshTokenCiphertext(String value) { this.refreshTokenCiphertext = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getConnectedEmail() { return connectedEmail; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
