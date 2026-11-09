package com.storch.smartjobs.integration;

import java.time.Instant;

public record OpportunityCandidate(String externalId, String subject, String content, Instant receivedAt) {
}
