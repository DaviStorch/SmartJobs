package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.repository.JobRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobServiceOwnershipTest {
    @Test
    void doesNotReturnAJobOwnedByAnotherAccount() {
        JobRepository jobs = mock(JobRepository.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(11L);
        when(jobs.findByIdAndUserId(99L, 11L)).thenReturn(Optional.empty());
        JobService service = new JobService(jobs, mock(AnalysisService.class));

        assertThrows(IllegalArgumentException.class, () -> service.ownedJob(99L, user));
        verify(jobs).findByIdAndUserId(99L, 11L);
    }
}
