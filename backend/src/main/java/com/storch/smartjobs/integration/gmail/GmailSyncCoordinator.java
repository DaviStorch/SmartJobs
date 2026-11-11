package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GmailSyncCoordinator {
    private static final long STALE_SYNC_SECONDS = 1800;
    private final GmailConnectionRepository connections;
    private final GmailSyncWorker worker;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public GmailSyncCoordinator(GmailConnectionRepository connections, GmailSyncWorker worker) {
        this.connections = connections;
        this.worker = worker;
    }

    public boolean enqueue(Long userId) {
        var connection = connections.findByUserId(userId)
                .orElseThrow(() -> new GmailIntegrationException("Conecte uma conta Gmail antes de sincronizar."));
        Instant startedAt = connection.getSyncStartedAt();
        if ("RUNNING".equals(connection.getSyncStatus()) && startedAt != null
                && startedAt.isAfter(Instant.now().minusSeconds(STALE_SYNC_SECONDS))) {
            return false;
        }
        if (!inFlight.add(userId)) return false;
        try {
            worker.synchronize(userId).whenComplete((result, failure) -> inFlight.remove(userId));
            return true;
        } catch (RuntimeException exception) {
            inFlight.remove(userId);
            throw exception;
        }
    }
}
