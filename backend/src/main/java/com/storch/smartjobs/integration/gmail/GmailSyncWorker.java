package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.entity.GmailConnection;
import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class GmailSyncWorker {
    private static final Logger logger = LoggerFactory.getLogger(GmailSyncWorker.class);
    private final GmailConnectionRepository connections;
    private final GmailOAuthService oauth;
    private final GmailSyncService sync;

    public GmailSyncWorker(GmailConnectionRepository connections, GmailOAuthService oauth, GmailSyncService sync) {
        this.connections = connections;
        this.oauth = oauth;
        this.sync = sync;
    }

    @Async("gmailSyncExecutor")
    public CompletableFuture<Void> synchronize(Long userId) {
        GmailConnection connection = connections.findByUserId(userId).orElse(null);
        if (connection == null) return CompletableFuture.completedFuture(null);
        var user = connection.getUser();
        oauth.markSyncStarted(user, java.time.Instant.now());
        try {
            sync.synchronize(user);
        } catch (GmailIntegrationException exception) {
            oauth.markSyncFailed(user, exception.getMessage());
            logger.warn("Gmail synchronization failed for SmartJobs user id {}: {}", userId, exception.getMessage());
        } catch (RuntimeException exception) {
            oauth.markSyncFailed(user, "Falha inesperada na sincronização Gmail.");
            logger.error("Unexpected Gmail synchronization failure for SmartJobs user id {}", userId, exception);
        }
        return CompletableFuture.completedFuture(null);
    }
}
