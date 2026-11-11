package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.GmailConnection;
import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GmailSyncWorkerTest {
    @Test
    void marksSyncStartedAndCompletesSuccessfully() {
        GmailConnectionRepository connections = mock(GmailConnectionRepository.class);
        GmailOAuthService oauth = mock(GmailOAuthService.class);
        GmailSyncService sync = mock(GmailSyncService.class);
        AppUser user = mock(AppUser.class);
        GmailConnection connection = mock(GmailConnection.class);
        when(connection.getUser()).thenReturn(user);
        when(connections.findByUserId(19L)).thenReturn(Optional.of(connection));
        GmailSyncWorker worker = new GmailSyncWorker(connections, oauth, sync);

        var result = worker.synchronize(19L);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        verify(oauth).markSyncStarted(eq(user), any());
        verify(sync).synchronize(user);
        verify(oauth, never()).markSyncFailed(any(), anyString());
    }

    @Test
    void recordsFailureAndCompletesTheBackgroundTask() {
        GmailConnectionRepository connections = mock(GmailConnectionRepository.class);
        GmailOAuthService oauth = mock(GmailOAuthService.class);
        GmailSyncService sync = mock(GmailSyncService.class);
        AppUser user = mock(AppUser.class);
        GmailConnection connection = mock(GmailConnection.class);
        when(connection.getUser()).thenReturn(user);
        when(connections.findByUserId(19L)).thenReturn(Optional.of(connection));
        doThrow(new GmailIntegrationException("Gmail unavailable")).when(sync).synchronize(user);
        GmailSyncWorker worker = new GmailSyncWorker(connections, oauth, sync);

        var result = worker.synchronize(19L);

        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        verify(oauth).markSyncFailed(user, "Gmail unavailable");
    }
}
