package com.storch.smartjobs.integration;

import com.storch.smartjobs.entity.AppUser;
import java.util.List;

public interface AuthorizedOpportunitySource {
    String providerId();

    List<OpportunityCandidate> findCandidates(AppUser user);
}
