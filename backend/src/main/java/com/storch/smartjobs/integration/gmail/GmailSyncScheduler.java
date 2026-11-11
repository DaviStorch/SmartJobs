package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.repository.GmailConnectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GmailSyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(GmailSyncScheduler.class);
    private final GmailConnectionRepository connections;
    private final GmailSyncCoordinator coordinator;
    private final boolean configured;

    public GmailSyncScheduler(GmailConnectionRepository connections, GmailSyncCoordinator coordinator,
                              @Value("${smartjobs.gmail.client-id:}") String clientId,
                              @Value("${smartjobs.gmail.client-secret:}") String clientSecret) {
        this.connections = connections;
        this.coordinator = coordinator;
        this.configured = !clientId.isBlank() && !clientSecret.isBlank();
    }

    @Scheduled(fixedDelayString = "${smartjobs.gmail.sync-interval-ms:900000}")
    public void enqueueConnectedAccounts() {
        if (!configured) return;
        connections.findAll().forEach(connection -> {
            try {
                coordinator.enqueue(connection.getUserId());
            } catch (GmailIntegrationException exception) {
                logger.debug("Skipped Gmail auto-sync for SmartJobs user id {}: {}", connection.getUserId(), exception.getMessage());
            }
        });
    }
}
