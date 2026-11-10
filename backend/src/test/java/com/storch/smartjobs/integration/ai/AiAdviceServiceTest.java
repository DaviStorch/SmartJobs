package com.storch.smartjobs.integration.ai;

import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.service.JobAnalysis;
import com.storch.smartjobs.service.RequirementMatch;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiAdviceServiceTest {
    @Test
    void disabledProviderFallsBackToDeterministicAnalysis() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.enabled()).thenReturn(false);
        when(provider.explain(any())).thenThrow(new AiUnavailableException("IA desativada."));
        AiAdviceService service = new AiAdviceService(provider);
        Job job = new Job();
        JobAnalysis analysis = new JobAnalysis(BigDecimal.valueOf(3.5), null, List.of(), List.of(), List.of(),
                List.of(), "Justificativa determinística", true);

        AiAdviceResult result = service.explain(1L, job, analysis, "pt");

        assertFalse(result.available());
        assertEquals("Justificativa determinística", result.text());
        assertTrue(result.notice().contains("continua disponível"));
    }

    @Test
    void passesExistingMatchesAndScoreToProviderWithoutChangingScore() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.enabled()).thenReturn(true);
        when(provider.explain(any())).thenAnswer(invocation -> {
            AiProvider.JobAdviceContext context = invocation.getArgument(0);
            assertEquals("4", context.score());
            assertTrue(context.matched().contains("Java"));
            assertTrue(context.missing().contains("Kubernetes"));
            assertTrue(context.jobText().contains("API"));
            return "Considere destacar sua experiência.";
        });
        Job job = new Job();
        job.setTitle("Developer");
        job.setCompany("Example");
        job.setDescription("API developer");
        JobAnalysis analysis = new JobAnalysis(BigDecimal.valueOf(4), null, List.of(
                new RequirementMatch("Java", RequirementMatch.Status.MET),
                new RequirementMatch("Kubernetes", RequirementMatch.Status.MISSING)),
                List.of("Java"), List.of("Kubernetes"), List.of(), "Base determinística", true);

        AiAdviceResult result = new AiAdviceService(provider).explain(1L, job, analysis, "en");

        assertTrue(result.available());
        assertEquals("Considere destacar sua experiência.", result.text());
    }

    @Test
    void throttlesRepeatedRequestsPerUserAndKeepsDeterministicFallback() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.enabled()).thenReturn(true);
        when(provider.explain(any())).thenReturn("Advice");
        AiAdviceService service = new AiAdviceService(provider);
        JobAnalysis analysis = new JobAnalysis(BigDecimal.ONE, null, List.of(), List.of(), List.of(),
                List.of(), "Deterministic", true);

        assertTrue(service.explain(42L, new Job(), analysis, "en").available());
        AiAdviceResult second = service.explain(42L, new Job(), analysis, "en");

        assertFalse(second.available());
        assertEquals("Deterministic", second.text());
        verify(provider, times(1)).explain(any());
    }
}
