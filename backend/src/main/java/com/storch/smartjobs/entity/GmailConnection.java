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

    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus = "IDLE";

    @Column(name = "sync_started_at")
    private Instant syncStartedAt;

    @Column(name = "last_sync_error", length = 500)
    private String lastSyncError;

    @Column(name = "last_sync_scanned", nullable = false)
    private int lastSyncScanned;

    @Column(name = "last_sync_imported", nullable = false)
    private int lastSyncImported;

    @Column(name = "last_sync_duplicates", nullable = false)
    private int lastSyncDuplicates;

    @Column(name = "last_sync_ignored", nullable = false)
    private int lastSyncIgnored;

    @Column(name = "last_sync_received_count", nullable = false)
    private int lastSyncReceivedCount;

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
    public AppUser getUser() { return user; }
    public String getAccessTokenCiphertext() { return accessTokenCiphertext; }
    public void setAccessTokenCiphertext(String value) { this.accessTokenCiphertext = value; }
    public String getRefreshTokenCiphertext() { return refreshTokenCiphertext; }
    public void setRefreshTokenCiphertext(String value) { this.refreshTokenCiphertext = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getConnectedEmail() { return connectedEmail; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
    public Instant getSyncStartedAt() { return syncStartedAt; }
    public void setSyncStartedAt(Instant syncStartedAt) { this.syncStartedAt = syncStartedAt; }
    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }
    public int getLastSyncScanned() { return lastSyncScanned; }
    public int getLastSyncImported() { return lastSyncImported; }
    public int getLastSyncDuplicates() { return lastSyncDuplicates; }
    public int getLastSyncIgnored() { return lastSyncIgnored; }
    public int getLastSyncReceivedCount() { return lastSyncReceivedCount; }

    public void completeSync(Instant syncedAt, int scanned, int receivedCount, int imported, int duplicates, int ignored) {
        this.lastSyncedAt = syncedAt;
        this.syncStatus = "SUCCESS";
        this.syncStartedAt = null;
        this.lastSyncError = null;
        this.lastSyncScanned = scanned;
        this.lastSyncReceivedCount = receivedCount;
        this.lastSyncImported = imported;
        this.lastSyncDuplicates = duplicates;
        this.lastSyncIgnored = ignored;
    }

    public void failSync(String error) {
        this.syncStatus = "ERROR";
        this.syncStartedAt = null;
        this.lastSyncError = error;
    }
}
