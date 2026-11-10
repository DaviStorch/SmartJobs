package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.entity.Recommendation;
import com.storch.smartjobs.entity.Resume;
import com.storch.smartjobs.entity.ResumeSection;
import com.storch.smartjobs.repository.ResumeRepository;
import com.storch.smartjobs.repository.ResumeSectionRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AnalysisService {
    private static final Pattern REQUIREMENT_SEPARATOR = Pattern.compile("[\\n\\r,;]+");
    private static final Set<String> IGNORED_WORDS = Set.of("com", "para", "uma", "the", "and", "of", "a", "o", "de", "em", "experiencia", "experience");

    private final ResumeRepository resumeRepository;
    private final ResumeSectionRepository sectionRepository;

    public AnalysisService(ResumeRepository resumeRepository, ResumeSectionRepository sectionRepository) {
        this.resumeRepository = resumeRepository;
        this.sectionRepository = sectionRepository;
    }

    public JobAnalysis analyze(Job job) {
        Optional<Resume> activeResume = resumeRepository.findFirstByUserIdAndActiveTrue(job.getUser().getId());
        if (activeResume.isEmpty()) {
            return new JobAnalysis(null, null, List.of(), List.of(),
                    List.of("Cadastre e ative um currículo para avaliar esta vaga."),
                    List.of("Adicione um currículo e mantenha suas competências atualizadas."),
                    "A compatibilidade não foi calculada porque não há currículo ativo.", false);
        }

        String resumeText = sectionRepository.findAllByResumeIdOrderByType(activeResume.get().getId()).stream()
                .map(ResumeSection::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        if (resumeText.isBlank()) {
            return new JobAnalysis(null, null, List.of(), List.of(),
                    List.of("O currículo ativo ainda não contém informações para comparar."),
                    List.of("Preencha ao menos as seções de resumo, experiência ou habilidades técnicas."),
                    "O currículo ativo está vazio; nenhuma compatibilidade foi estimada.", false);
        }
        List<String> parsedRequirements = REQUIREMENT_SEPARATOR.splitAsStream(
                        Objects.requireNonNullElse(job.getRequirements(), ""))
                .map(String::strip)
                .map(value -> value.replaceFirst("^[\\s•*\\-]+", "").strip())
                .filter(value -> value.length() > 1)
                .distinct()
                .toList();

        List<RequirementMatch> matches = parsedRequirements.stream()
                .map(requirement -> new RequirementMatch(requirement, match(requirement, resumeText)))
                .toList();
        long met = matches.stream().filter(match -> match.status() == RequirementMatch.Status.MET).count();
        long partial = matches.stream().filter(match -> match.status() == RequirementMatch.Status.PARTIAL).count();
        BigDecimal score = matches.isEmpty() ? null
                : BigDecimal.valueOf((met + partial * 0.5) * 5.0 / matches.size())
                    .setScale(2, RoundingMode.HALF_UP);
        Recommendation recommendation = score == null ? Recommendation.CONSIDERAR
                : score.compareTo(BigDecimal.valueOf(4)) >= 0 ? Recommendation.APLICAR
                : score.compareTo(BigDecimal.valueOf(2.5)) >= 0 ? Recommendation.CONSIDERAR : Recommendation.EVITAR;
        List<String> positives = matches.stream()
                .filter(match -> match.status() == RequirementMatch.Status.MET)
                .map(RequirementMatch::requirement).toList();
        List<String> negatives = matches.stream()
                .filter(match -> match.status() == RequirementMatch.Status.MISSING)
                .map(RequirementMatch::requirement).toList();
        List<String> suggestions = negatives.stream().limit(5)
                .map(requirement -> "Se você possui esta experiência, descreva-a no currículo: " + requirement)
                .toList();
        String rationale = matches.isEmpty()
                ? "A vaga não informa requisitos estruturados; não há dados suficientes para calcular uma nota."
                : "Nota determinística: requisito atendido vale 1 ponto, parcial 0,5; correspondência de 75% ou mais é atendida e de 35% a 74% é parcial. Aplicar ≥ 4,00; considerar ≥ 2,50; evitar abaixo disso. Nenhuma qualificação é inferida além do currículo.";
        return new JobAnalysis(score, recommendation, matches, positives, negatives, suggestions, rationale, true);
    }

    private RequirementMatch.Status match(String requirement, String resume) {
        String normalizedRequirement = normalize(requirement);
        Set<String> resumeWords = Arrays.stream(normalize(resume).split("\\s+"))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toSet());
        if (normalizedRequirement.isBlank()) {
            return RequirementMatch.Status.MET;
        }
        Set<String> requiredWords = Arrays.stream(normalizedRequirement.split("\\s+"))
                .filter(word -> word.length() > 1 && !IGNORED_WORDS.contains(word))
                .collect(Collectors.toSet());
        if (requiredWords.isEmpty()) return RequirementMatch.Status.MISSING;
        if ((" " + String.join(" ", resumeWords) + " ").contains(" " + normalizedRequirement + " ")
                || resumeWords.containsAll(requiredWords)) return RequirementMatch.Status.MET;
        long overlap = requiredWords.stream().filter(resumeWords::contains).count();
        double ratio = (double) overlap / requiredWords.size();
        if (ratio >= 0.75) return RequirementMatch.Status.MET;
        if (ratio >= 0.35) return RequirementMatch.Status.PARTIAL;
        return RequirementMatch.Status.MISSING;
    }

    private String normalize(String text) {
        return Normalizer.normalize(Objects.requireNonNullElse(text, ""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
