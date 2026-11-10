package com.storch.smartjobs.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JobDuplicateCheckerTest {
    @Test
    void recognizesSameOpportunityDespiteTrackingParametersAndTrailingSlash() {
        assertTrue(JobDuplicateChecker.equivalent(
                "https://jobs.example.com/role/123/?utm_source=mail&ref=homepage",
                "https://jobs.example.com/role/123",
                "North", "North", "Designer", "Different title"));
    }

    @Test
    void recognizesNormalizedCompanyAndTitleWithoutUrl() {
        assertTrue(JobDuplicateChecker.equivalent(
                null, null, "Companhia Ágil", "companhia agil", "Engenheiro de Dados", " engenheiro-de-dados "));
    }

    @Test
    void doesNotMatchDifferentOpportunities() {
        assertFalse(JobDuplicateChecker.equivalent(
                "https://jobs.example.com/1", "https://jobs.example.com/2",
                "Acme", "Other", "Backend", "Frontend"));
    }
}
