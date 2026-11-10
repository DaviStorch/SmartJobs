package com.storch.smartjobs.integration.ai;

import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.service.JobAnalysis;
import com.storch.smartjobs.service.RequirementMatch;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiAdviceService {
    private final AiProvider provider;
    private final ConcurrentHashMap<Long, Long> lastRequests = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MILLIS = 30_000;

    public AiAdviceService(AiProvider provider) {
        this.provider = provider;
    }

    public AiAdviceResult explain(Long userId, Job job, JobAnalysis analysis, String language) {
        if (provider.enabled() && isRateLimited(userId)) {
            return new AiAdviceResult(false, analysis.rationale(),
                    "Aguarde 30 segundos entre solicitações de IA. A análise determinística continua disponível.");
        }
        List<String> matched = analysis.requirements().stream()
                .filter(item -> item.status() == RequirementMatch.Status.MET || item.status() == RequirementMatch.Status.PARTIAL)
                .map(RequirementMatch::requirement).toList();
        List<String> missing = analysis.requirements().stream()
                .filter(item -> item.status() == RequirementMatch.Status.MISSING)
                .map(RequirementMatch::requirement).toList();
        String vacancyText = String.join("\n", List.of(
                value(job.getDescription()), value(job.getRequirements()), value(job.getBenefits())));
        try {
            String answer = provider.explain(new AiProvider.JobAdviceContext(
                    job.getTitle(), job.getCompany(), vacancyText, analysis.score().toPlainString(), language,
                    matched, missing, analysis.rationale()));
            return new AiAdviceResult(true, answer, "A pontuação acima continua sendo calculada pelo algoritmo do SmartJobs.");
        } catch (AiUnavailableException exception) {
            return new AiAdviceResult(false, analysis.rationale(),
                    exception.getMessage() + " A análise determinística continua disponível.");
        }
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    public boolean enabled() {
        return provider.enabled();
    }

    private boolean isRateLimited(Long userId) {
        long now = System.currentTimeMillis();
        lastRequests.entrySet().removeIf(entry -> now - entry.getValue() > RATE_LIMIT_MILLIS * 10);
        java.util.concurrent.atomic.AtomicBoolean limited = new java.util.concurrent.atomic.AtomicBoolean();
        lastRequests.compute(userId, (key, previous) -> {
            if (previous != null && now - previous < RATE_LIMIT_MILLIS) {
                limited.set(true);
                return previous;
            }
            return now;
        });
        return limited.get();
    }
}
