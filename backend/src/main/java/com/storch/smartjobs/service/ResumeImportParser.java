package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.ResumeSectionType;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResumeImportParser {
    private static final Map<ResumeSectionType, List<String>> HEADINGS = Map.of(
            ResumeSectionType.PERSONAL_INFO, List.of("dados pessoais", "informacoes pessoais", "personal information", "contact", "contato"),
            ResumeSectionType.PROFESSIONAL_SUMMARY, List.of("resumo profissional", "professional summary", "summary", "profile", "perfil", "sobre mim", "about me"),
            ResumeSectionType.OBJECTIVES, List.of("objetivo", "objetivos", "objective", "career objective"),
            ResumeSectionType.EDUCATION, List.of("formacao academica", "educacao", "education", "academic background"),
            ResumeSectionType.EXPERIENCE, List.of("experiencia profissional", "experiencia", "professional experience", "work experience", "employment history"),
            ResumeSectionType.PROJECTS, List.of("projetos", "projects", "portfolio"),
            ResumeSectionType.TECHNICAL_SKILLS, List.of("habilidades tecnicas", "competencias tecnicas", "skills", "technical skills", "technologies", "tecnologias", "conhecimentos"),
            ResumeSectionType.LANGUAGES, List.of("idiomas", "languages", "linguas"),
            ResumeSectionType.CERTIFICATIONS, List.of("certificacoes", "certificates", "certifications", "cursos", "courses", "cursos e certificacoes")
    );
    private static final Pattern EMAIL = Pattern.compile("(?i)[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d ()-]{7,}\\d)(?!\\d)");
    private static final Pattern URL = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?(?:linkedin\\.com|github\\.com)/[^\\s,;]+|https?://[^\\s,;]+");
    private static final Pattern SKILL = Pattern.compile("(?i)(?<![\\w])(java|javascript|typescript|python|kotlin|golang|c\\+\\+|c#|ruby|php|swift|scala|rust|sql|postgres(?:ql)?|mysql|mariadb|sqlite|mongodb|dynamodb|cassandra|redis|oracle|elasticsearch|kafka|rabbitmq|spring(?: boot| cloud)?|react|angular|vue(?:\\.js)?|node(?:\\.js)?|express|django|flask|fastapi|\\.net|graphql|html5?|css3?|aws|azure|gcp|docker|kubernetes|git(?:hub|lab)?|linux|terraform|jenkins|maven|gradle|postman|jira|confluence|power bi|tableau|figma|pandas|numpy|pytorch|tensorflow)(?![\\w])");
    private static final List<String> PERSONAL_KEYS = List.of("linkedin", "github", "portfolio", "portifolio", "email", "telefone", "phone", "cidade", "city");

    public ResumeImportDraft parse(String extractedText, String warning) {
        EnumMap<ResumeSectionType, StringBuilder> sections = new EnumMap<>(ResumeSectionType.class);
        HEADINGS.keySet().forEach(type -> sections.put(type, new StringBuilder()));
        ResumeSectionType current = null;
        List<String> unclassified = new ArrayList<>();

        for (String line : Objects.requireNonNullElse(extractedText, "").replace('\u0000', ' ').lines().toList()) {
            String trimmed = line.strip();
            if (trimmed.isBlank()) {
                if (current != null) append(sections.get(current), "");
                continue;
            }
            HeadingMatch heading = resolveHeading(trimmed);
            if (heading != null) {
                current = heading.type();
                if (!heading.content().isBlank()) append(sections.get(current), heading.content());
                continue;
            }
            if (current == null) unclassified.add(trimmed);
            else append(sections.get(current), trimmed);
        }

        String header = String.join("\n", unclassified);
        String contacts = extractContacts(header);
        if (!contacts.isBlank()) append(sections.get(ResumeSectionType.PERSONAL_INFO), contacts);
        String skillEvidence = Objects.requireNonNullElse(extractedText, "") + "\n"
                + String.join("\n", sections.values().stream().map(StringBuilder::toString).toList());
        Set<String> skills = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Matcher matcher = SKILL.matcher(skillEvidence);
        while (matcher.find()) skills.add(matcher.group());
        String explicitSkills = sections.get(ResumeSectionType.TECHNICAL_SKILLS).toString().strip();
        if (explicitSkills.isBlank() && !skills.isEmpty()) {
            sections.get(ResumeSectionType.TECHNICAL_SKILLS).append(String.join(", ", skills));
        }

        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (ResumeSectionType type : ResumeSectionType.values()) {
            if (type == ResumeSectionType.IMPORTED_TEXT) {
                result.put(type.name(), Objects.requireNonNullElse(extractedText, ""));
            } else {
                result.put(type.name(), sections.get(type).toString().strip());
            }
        }
        String finalWarning = warning;
        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (entry.getValue().length() > 30000) {
                entry.setValue(entry.getValue().substring(0, 30000));
                finalWarning = appendWarning(finalWarning, "Um ou mais módulos excederam 30.000 caracteres e foram limitados. Confira o texto original antes de salvar.");
            }
        }
        return new ResumeImportDraft(Collections.unmodifiableMap(result), finalWarning);
    }

    private String appendWarning(String current, String message) {
        return current == null || current.isBlank() ? message : current + " " + message;
    }

    private HeadingMatch resolveHeading(String line) {
        String[] parts = line.split("\\s*[:–—-]\\s*", 2);
        String normalized = normalize(parts[0]);
        for (Map.Entry<ResumeSectionType, List<String>> entry : HEADINGS.entrySet()) {
            for (String heading : entry.getValue()) {
                if (normalized.equals(heading)) {
                    return new HeadingMatch(entry.getKey(), parts.length > 1 ? parts[1].strip() : "");
                }
            }
        }
        return null;
    }

    private record HeadingMatch(ResumeSectionType type, String content) {
    }

    private String extractContacts(String header) {
        List<String> result = new ArrayList<>();
        Matcher email = EMAIL.matcher(header);
        if (email.find()) result.add("E-mail: " + email.group());
        Matcher phone = PHONE.matcher(header);
        if (phone.find()) result.add("Telefone: " + phone.group().strip());
        Matcher url = URL.matcher(header);
        while (url.find()) {
            String value = url.group();
            if (!value.startsWith("http")) value = "https://" + value;
            result.add(value);
        }
        for (String line : header.lines().toList()) {
            if (PERSONAL_KEYS.stream().anyMatch(key -> normalize(line).startsWith(key))) result.add(line);
        }
        return result.stream().distinct().reduce((a, b) -> a + "\n" + b).orElse("");
    }

    private void append(StringBuilder target, String value) {
        if (!target.isEmpty()) target.append('\n');
        target.append(value);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+.#]+", " ").trim().replaceAll("\\s+", " ");
    }
}
