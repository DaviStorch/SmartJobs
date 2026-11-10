package com.storch.smartjobs.integration.gmail;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GmailMessageParser {
    private static final Pattern TITLE_LABEL = Pattern.compile("(?im)^[ \\t]*(?:job title|position|cargo|vaga)[ \\t]*[:\\-][ \\t]*(.{2,180})[ \\t]*$");
    private static final Pattern COMPANY_LABEL = Pattern.compile("(?im)^[ \\t]*(?:company|empresa|hiring company)[ \\t]*[:\\-][ \\t]*(.{2,160})[ \\t]*$");
    private static final Pattern LOCATION_LABEL = Pattern.compile("(?im)^[ \\t]*(?:location|local|localização|localizacao)[ \\t]*[:\\-][ \\t]*(.{2,160})[ \\t]*$");
    private static final Pattern REQUIREMENTS_SECTION = Pattern.compile("(?ims)^[ \\t]*(?:requirements?|requisitos?|qualifications?|qualificações|qualificacoes)[ \\t]*[:\\-]?[ \\t]*\\n?(.*?)(?=^[ \\t]*(?:benefits?|benefícios|beneficios|location|localização|localizacao|salary|salário|salario|about the role|sobre a vaga)[ \\t]*[:\\-]?|\\z)");
    private static final Pattern BENEFITS_SECTION = Pattern.compile("(?ims)^[ \\t]*(?:benefits?|benefícios|beneficios)[ \\t]*[:\\-]?[ \\t]*\\n?(.*?)(?=^[ \\t]*(?:requirements?|requisitos?|qualifications?|qualificações|qualificacoes|location|localização|localizacao|salary|salário|salario|about the role|sobre a vaga)[ \\t]*[:\\-]?|\\z)");
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_LINK = Pattern.compile("(?is)<a\\b[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>(.*?)</a>");
    private static final List<String> JOB_TERMS = List.of("job", "jobs", "employment", "career", "vacancy",
            "opportunit", "emprego", "vaga", "trabalho", "recrut");

    public Optional<ParsedOpportunity> parse(String messageId, String subject, String body) {
        String titleSource = clean(subject);
        String text = stripHtml(body);
        if (JOB_TERMS.stream().noneMatch(term -> (titleSource + " " + text).toLowerCase(Locale.ROOT).contains(term))) {
            return Optional.empty();
        }

        String title = firstMatch(TITLE_LABEL, text);
        String company = firstMatch(COMPANY_LABEL, text);
        if (title == null) {
            title = titleSource.replaceFirst("(?i)^(?:job alert|new jobs?|empregos?|vagas?|oportunidades?)\\s*[:\\-–]?\\s*", "").strip();
            Matcher at = Pattern.compile("(?i)^(.{2,130}?)\\s+(?:at|na|no|em)\\s+(.{2,130})$").matcher(title);
            if (at.matches()) {
                title = at.group(1).strip();
                if (company == null) company = trimSubjectSuffix(at.group(2));
            }
        }
        if (title == null || title.isBlank() || title.length() > 180 || company == null || company.isBlank()) {
            return Optional.empty();
        }
        String link = findOpportunityLink(body);
        return Optional.of(new ParsedOpportunity(messageId, title, trimSubjectSuffix(company), text, link,
                firstMatch(LOCATION_LABEL, text), firstMatch(REQUIREMENTS_SECTION, text),
                firstMatch(BENEFITS_SECTION, text)));
    }

    public String findEmail(Map<?, ?> message) {
        return header(message, "From");
    }

    public String findSubject(Map<?, ?> message) {
        return header(message, "Subject");
    }

    public String extractBody(Map<?, ?> payload) {
        String plain = collectText(payload, "text/plain");
        if (!plain.isBlank()) return plain;
        return stripHtml(collectText(payload, "text/html"));
    }

    private String collectText(Map<?, ?> part, String desiredMime) {
        String mimeType = Objects.toString(part.get("mimeType"), "");
        if (desiredMime.equalsIgnoreCase(mimeType)) {
            Object bodyValue = part.get("body");
            if (bodyValue instanceof Map<?, ?> body && body.get("data") instanceof String data) {
                try {
                    return new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException ignored) {
                    return "";
                }
            }
        }
        Object parts = part.get("parts");
        if (parts instanceof List<?> list) {
            for (Object child : list) {
                if (child instanceof Map<?, ?> childPart) {
                    String value = collectText(childPart, desiredMime);
                    if (!value.isBlank()) return value;
                }
            }
        }
        return "";
    }

    private String header(Map<?, ?> message, String name) {
        Object payload = message.get("payload");
        if (!(payload instanceof Map<?, ?> payloadMap) || !(payloadMap.get("headers") instanceof List<?> headers)) return "";
        for (Object header : headers) {
            if (header instanceof Map<?, ?> values && name.equalsIgnoreCase(Objects.toString(values.get("name"), ""))) {
                return Objects.toString(values.get("value"), "");
            }
        }
        return "";
    }

    private String findOpportunityLink(String body) {
        List<String> links = new ArrayList<>();
        Matcher html = HTML_LINK.matcher(body);
        while (html.find()) {
            String href = html.group(1);
            String label = stripHtml(html.group(2)).toLowerCase(Locale.ROOT);
            if (isCandidateLink(href) && (hasJobTerm(label) || isCareerHost(href))) links.add(href);
        }
        Matcher plain = URL.matcher(body);
        while (plain.find()) {
            String candidate = plain.group().replaceAll("[),.;]+$", "");
            if (isCandidateLink(candidate) && isCareerHost(candidate)) links.add(candidate);
        }
        return links.stream().findFirst().orElse(null);
    }

    private boolean isCandidateLink(String candidate) {
        String value = candidate.toLowerCase(Locale.ROOT);
        return value.startsWith("https://") && !value.contains("unsubscribe")
                && !value.contains("google.com/preferences") && !value.contains("mail.google.com");
    }

    private boolean isCareerHost(String candidate) {
        String value = candidate.toLowerCase(Locale.ROOT);
        return List.of("linkedin.com/jobs", "indeed.", "greenhouse.io", "lever.co", "workday.", "jobs.", "/careers", "/career", "/jobs", "/job/")
                .stream().anyMatch(value::contains);
    }

    private boolean hasJobTerm(String value) {
        return JOB_TERMS.stream().anyMatch(value::contains);
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? trimSubjectSuffix(matcher.group(1)) : null;
    }

    private String trimSubjectSuffix(String value) {
        return value.replaceAll("(?i)\\s*(?:\\||—|–|-)\\s*(?:linkedin|indeed|glassdoor|job alert).*$", "")
                .replaceAll("\\s{2,}", " ").strip();
    }

    private String stripHtml(String value) {
        return Objects.requireNonNullElse(value, "").replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</li>", "\n")
                .replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n").strip();
    }

    private String clean(String value) {
        return Objects.requireNonNullElse(value, "").replaceAll("\\s+", " ").strip();
    }

    public record ParsedOpportunity(String messageId, String title, String company, String description, String sourceUrl,
                                    String location, String requirements, String benefits) {
    }
}
