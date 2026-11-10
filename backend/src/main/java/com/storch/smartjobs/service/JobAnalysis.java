package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.Recommendation;
import java.math.BigDecimal;
import java.util.List;

public record JobAnalysis(
        BigDecimal score,
        Recommendation recommendation,
        List<RequirementMatch> requirements,
        List<String> positives,
        List<String> negatives,
        List<String> suggestions,
        String rationale,
        boolean resumeAvailable) {
}
