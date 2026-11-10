package com.storch.smartjobs.integration.gmail;

public class GmailIntegrationException extends RuntimeException {
    public GmailIntegrationException(String message) {
        super(message);
    }

    public GmailIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
