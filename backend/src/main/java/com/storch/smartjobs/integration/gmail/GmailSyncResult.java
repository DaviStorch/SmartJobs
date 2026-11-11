package com.storch.smartjobs.integration.gmail;

public record GmailSyncResult(int scanned, int receivedCount, int imported, int duplicates, int ignored) {
}
