package com.storch.smartjobs.integration.gmail;

import com.storch.smartjobs.dto.JobForm;
import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.GmailImportedMessage;
import com.storch.smartjobs.repository.GmailImportedMessageRepository;
import com.storch.smartjobs.service.JobService;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class GmailSyncService {
    private static final String MESSAGE_LIST_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages";
    private static final int MAX_MESSAGES_PER_SYNC = 50;
    private final GmailOAuthService oauth;
    private final GmailImportedMessageRepository importedMessages;
    private final JobService jobs;
    private final GmailMessageParser parser = new GmailMessageParser();

    public GmailSyncService(GmailOAuthService oauth, GmailImportedMessageRepository importedMessages, JobService jobs) {
        this.oauth = oauth;
        this.importedMessages = importedMessages;
        this.jobs = jobs;
    }

    public GmailSyncResult synchronize(AppUser user) {
        String accessToken = oauth.accessToken(user);
        Instant syncStarted = Instant.now();
        Instant lastSync = oauth.connection(user).orElseThrow().getLastSyncedAt();
        String query = lastSync == null
                ? "newer_than:30d {subject:job subject:jobs subject:employment subject:emprego subject:vaga subject:opportunity subject:career}"
                : "after:" + lastSync.getEpochSecond()
                + " {subject:job subject:jobs subject:employment subject:emprego subject:vaga subject:opportunity subject:career}";
        List<String> ids = listMessageIds(accessToken, query);
        int imported = 0;
        int duplicates = 0;
        int ignored = 0;
        int scanned = 0;
        for (String id : ids) {
            if (importedMessages.existsByUserIdAndMessageId(user.getId(), id)) continue;
            Map<?, ?> message = oauth.apiGet(MESSAGE_LIST_URL + "/" + enc(id) + "?format=full", accessToken);
            String subject = parser.findSubject(message);
            String body = parser.extractBody(map(message.get("payload")));
            scanned++;
            Optional<GmailMessageParser.ParsedOpportunity> candidate = parser.parse(id, subject, body);
            if (candidate.isEmpty()) {
                importedMessages.save(new GmailImportedMessage(user, id, "IGNORED"));
                ignored++;
                continue;
            }
            GmailMessageParser.ParsedOpportunity opportunity = candidate.get();
            JobForm form = new JobForm();
            form.setTitle(opportunity.title());
            form.setCompany(opportunity.company());
            form.setDescription(opportunity.description().substring(0, Math.min(20_000, opportunity.description().length())));
            form.setLocation(opportunity.location());
            form.setRequirements(limit(opportunity.requirements(), 10_000));
            form.setBenefits(limit(opportunity.benefits(), 10_000));
            form.setSource("Gmail");
            form.setSourceUrl(opportunity.sourceUrl());
            form.setStatus("PENDENTE");
            boolean saved = jobs.saveImported(user, form);
            importedMessages.save(new GmailImportedMessage(user, id, saved ? "IMPORTED" : "DUPLICATE"));
            if (saved) imported++;
            else duplicates++;
        }
        oauth.markSynced(user, syncStarted);
        return new GmailSyncResult(scanned, imported, duplicates, ignored);
    }

    private List<String> listMessageIds(String accessToken, String query) {
        List<String> ids = new ArrayList<>();
        String pageToken = null;
        while (ids.size() < MAX_MESSAGES_PER_SYNC) {
            String uri = MESSAGE_LIST_URL + "?maxResults=" + Math.min(25, MAX_MESSAGES_PER_SYNC - ids.size())
                    + "&q=" + enc(query) + (pageToken == null ? "" : "&pageToken=" + enc(pageToken));
            Map<?, ?> response = oauth.apiGet(uri, accessToken);
            Object messages = response.get("messages");
            if (!(messages instanceof List<?> list) || list.isEmpty()) break;
            for (Object value : list) {
                if (value instanceof Map<?, ?> message && message.get("id") != null) ids.add(message.get("id").toString());
                if (ids.size() == MAX_MESSAGES_PER_SYNC) break;
            }
            Object nextPage = response.get("nextPageToken");
            if (nextPage == null || nextPage.toString().isBlank()) break;
            pageToken = nextPage.toString();
        }
        return ids;
    }

    private Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> result ? result : Map.of();
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String limit(String value, int length) {
        return value == null ? null : value.substring(0, Math.min(value.length(), length));
    }
}
