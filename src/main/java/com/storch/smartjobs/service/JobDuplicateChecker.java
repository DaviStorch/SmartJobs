package com.storch.smartjobs.service;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class JobDuplicateChecker {
    private static final Set<String> TRACKING_PARAMETERS = Set.of("utm_source", "utm_medium", "utm_campaign",
            "utm_term", "utm_content", "trk", "ref");

    private JobDuplicateChecker() {
    }

    public static boolean equivalent(String firstUrl, String secondUrl,
                                     String firstCompany, String secondCompany,
                                     String firstTitle, String secondTitle) {
        String normalizedFirstUrl = normalizeUrl(firstUrl);
        String normalizedSecondUrl = normalizeUrl(secondUrl);
        if (!normalizedFirstUrl.isEmpty() && normalizedFirstUrl.equals(normalizedSecondUrl)) {
            return true;
        }
        return normalizeText(firstCompany).equals(normalizeText(secondCompany))
                && normalizeText(firstTitle).equals(normalizeText(secondTitle));
    }

    static String normalizeUrl(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            String query = uri.getQuery();
            if (query != null) {
                query = java.util.Arrays.stream(query.split("&"))
                        .filter(parameter -> !TRACKING_PARAMETERS.contains(parameter.split("=", 2)[0].toLowerCase(Locale.ROOT)))
                        .sorted()
                        .collect(java.util.stream.Collectors.joining("&"));
            }
            return host + (port < 0 || port == 80 || port == 443 ? "" : ":" + port) + path
                    + (query == null || query.isBlank() ? "" : "?" + query);
        } catch (IllegalArgumentException exception) {
            return value.trim().toLowerCase(Locale.ROOT);
        }
    }

    static String normalizeText(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
