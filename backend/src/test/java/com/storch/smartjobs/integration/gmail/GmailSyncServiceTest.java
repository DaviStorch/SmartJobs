package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.GmailConnection;
import com.storch.smartjobs.repository.GmailImportedMessageRepository;
import com.storch.smartjobs.service.JobService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GmailSyncServiceTest {
    @Test
    void importsOnlyParsedMessagesAndRecordsIdempotency() {
        GmailOAuthService oauth = mock(GmailOAuthService.class);
        GmailImportedMessageRepository imported = mock(GmailImportedMessageRepository.class);
        JobService jobs = mock(JobService.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(7L);
        GmailConnection connection = mock(GmailConnection.class);
        when(connection.getLastSyncedAt()).thenReturn(null);
        when(oauth.accessToken(user)).thenReturn("access-token");
        when(oauth.connection(user)).thenReturn(java.util.Optional.of(connection));
        doReturn(Map.of("messages", List.of(Map.of("id", "message-1"))))
                .when(oauth).apiGet(contains("maxResults="), eq("access-token"));
        doReturn(Map.of(
                "payload", Map.of(
                        "mimeType", "text/plain",
                        "body", Map.of("data", java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                                ("Job title: Developer\nCompany: Example Corp\nRequirements:\nJava\nSee this job: https://example.com/jobs/developer")
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                        "headers", List.of(Map.of("name", "Subject", "value", "Job alert: Developer")))))
                .when(oauth).apiGet(contains("message-1?format=full"), eq("access-token"));
        when(imported.existsByUserIdAndMessageId(7L, "message-1")).thenReturn(false);
        when(jobs.saveImported(eq(user), any())).thenReturn(true);

        GmailSyncResult result = new GmailSyncService(oauth, imported, jobs).synchronize(user);

        assertEquals(new GmailSyncResult(1, 1, 0, 0), result);
        verify(jobs).saveImported(eq(user), argThat(form -> form.getSource().equals("Gmail")
                && form.getCompany().equals("Example Corp")
                && form.getRequirements().contains("Java")
                && form.getSourceUrl().equals("https://example.com/jobs/developer")));
        verify(imported).save(any());
        verify(oauth).markSynced(eq(user), any(Instant.class));
    }

    @Test
    void skipsMessagesAlreadyProcessedWithoutFetchingThemAgain() {
        GmailOAuthService oauth = mock(GmailOAuthService.class);
        GmailImportedMessageRepository imported = mock(GmailImportedMessageRepository.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(7L);
        GmailConnection connection = mock(GmailConnection.class);
        when(connection.getLastSyncedAt()).thenReturn(null);
        when(oauth.accessToken(user)).thenReturn("token");
        when(oauth.connection(user)).thenReturn(java.util.Optional.of(connection));
        doReturn(Map.of("messages", List.of(Map.of("id", "seen"))))
                .when(oauth).apiGet(contains("maxResults="), eq("token"));
        when(imported.existsByUserIdAndMessageId(7L, "seen")).thenReturn(true);

        GmailSyncResult result = new GmailSyncService(oauth, imported, mock(JobService.class)).synchronize(user);

        assertEquals(0, result.scanned());
        verify(oauth, times(1)).apiGet(contains("maxResults="), eq("token"));
    }
}
