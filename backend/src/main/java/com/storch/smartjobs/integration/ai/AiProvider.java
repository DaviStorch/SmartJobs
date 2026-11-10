package com.storch.smartjobs.integration.ai;

import java.util.List;

public interface AiProvider {
    String explain(JobAdviceContext context);
    boolean enabled();

    record JobAdviceContext(String title, String company, String jobText, String score, String language,
                            List<String> matched, List<String> missing, String deterministicRationale) {
    }
}
