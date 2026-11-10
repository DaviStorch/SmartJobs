package com.storch.smartjobs.integration.gmail;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GmailMessageParserTest {
    private final GmailMessageParser parser = new GmailMessageParser();

    @Test
    void parsesOnlyJobMessagesWithACompanyAndKeepsTheOriginalLink() {
        var result = parser.parse("id-1", "Job alert: Backend Developer", """
                <p>Job title: Backend Developer</p>
                <p>Company: Example Corp</p>
                <p>Location: Remote</p>
                <p>Requirements:</p><ul><li>Java</li><li>PostgreSQL</li></ul>
                <p>Benefits:</p><p>Health plan</p>
                <a href="https://example.com/careers/backend">View job</a>
                """);

        assertTrue(result.isPresent());
        assertEquals("Backend Developer", result.get().title());
        assertEquals("Example Corp", result.get().company());
        assertEquals("https://example.com/careers/backend", result.get().sourceUrl());
        assertTrue(result.get().description().contains("Java"));
        assertTrue(result.get().description().contains("PostgreSQL"));
        assertEquals("Remote", result.get().location());
        assertTrue(result.get().requirements().contains("PostgreSQL"));
        assertTrue(result.get().benefits().contains("Health plan"));
    }

    @Test
    void ignoresUnrelatedMailAndOpportunitiesWithoutIdentifiableCompany() {
        assertTrue(parser.parse("id-2", "Your monthly invoice", "Attached is your receipt.").isEmpty());
        assertTrue(parser.parse("id-3", "Job alert: Backend Developer", "Job alert for a new position.").isEmpty());
    }

    @Test
    void extractsPlainTextFromNestedGmailMimeParts() {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("New job alert".getBytes(StandardCharsets.UTF_8));
        Map<String, Object> plain = Map.of("mimeType", "text/plain", "body", Map.of("data", encoded));
        Map<String, Object> payload = Map.of("mimeType", "multipart/alternative", "parts", List.of(plain));
        assertEquals("New job alert", parser.extractBody(payload));
    }
}
