package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.entity.GmailConnection;
import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GmailSyncCoordinatorTest {
    @Test
    void preventsConcurrentRequestsAndReleasesInFlightStateWhenWorkerCompletes() {
        GmailConnectionRepository connections = mock(GmailConnectionRepository.class);
        GmailSyncWorker worker = mock(GmailSyncWorker.class);
        GmailConnection connection = mock(GmailConnection.class);
        CompletableFuture<Void> task = new CompletableFuture<>();
        when(connections.findByUserId(12L)).thenReturn(Optional.of(connection));
        when(connection.getSyncStatus()).thenReturn("IDLE");
        when(worker.synchronize(12L)).thenReturn(task);
        GmailSyncCoordinator coordinator = new GmailSyncCoordinator(connections, worker);

        assertTrue(coordinator.enqueue(12L));
        assertFalse(coordinator.enqueue(12L));
        task.complete(null);
        assertTrue(coordinator.enqueue(12L));
    }

    @Test
    void releasesInFlightStateWhenWorkerCompletesExceptionally() {
        GmailConnectionRepository connections = mock(GmailConnectionRepository.class);
        GmailSyncWorker worker = mock(GmailSyncWorker.class);
        GmailConnection connection = mock(GmailConnection.class);
        CompletableFuture<Void> task = new CompletableFuture<>();
        when(connections.findByUserId(12L)).thenReturn(Optional.of(connection));
        when(connection.getSyncStatus()).thenReturn("IDLE");
        when(worker.synchronize(12L)).thenReturn(task);
        GmailSyncCoordinator coordinator = new GmailSyncCoordinator(connections, worker);

        assertTrue(coordinator.enqueue(12L));
        task.completeExceptionally(new IllegalStateException("worker failed"));

        assertTrue(coordinator.enqueue(12L));
    }

    @Test
    void rejectsRecentPersistedRunningSynchronization() {
        GmailConnectionRepository connections = mock(GmailConnectionRepository.class);
        GmailSyncWorker worker = mock(GmailSyncWorker.class);
        GmailConnection connection = mock(GmailConnection.class);
        when(connections.findByUserId(12L)).thenReturn(Optional.of(connection));
        when(connection.getSyncStatus()).thenReturn("RUNNING");
        when(connection.getSyncStartedAt()).thenReturn(Instant.now());
        GmailSyncCoordinator coordinator = new GmailSyncCoordinator(connections, worker);

        assertFalse(coordinator.enqueue(12L));
        verifyNoInteractions(worker);
    }
}
