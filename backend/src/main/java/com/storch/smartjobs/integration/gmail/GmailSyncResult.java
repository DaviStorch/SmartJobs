package com.storch.smartjobs.integration.gmail;

public record GmailSyncResult(int scanned, int imported, int duplicates, int ignored) {
}
