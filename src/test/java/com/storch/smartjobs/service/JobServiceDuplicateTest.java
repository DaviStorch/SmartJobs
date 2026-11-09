package com.storch.smartjobs.service;

import com.storch.smartjobs.dto.JobForm;
import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.repository.JobRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JobServiceDuplicateTest {
    @Test
    void preventsDuplicateSourceLinksEvenWhenCompanyAndTitleDiffer() {
        JobRepository jobs = mock(JobRepository.class);
        AnalysisService analysis = mock(AnalysisService.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(4L);
        Job existing = mock(Job.class);
        when(existing.getId()).thenReturn(12L);
        when(existing.getSourceUrl()).thenReturn("https://jobs.example.com/role/42?utm_source=mail");
        when(existing.getCompany()).thenReturn("First Company");
        when(existing.getTitle()).thenReturn("First title");
        when(jobs.findAllByUserId(4L)).thenReturn(List.of(existing));
        JobForm form = new JobForm();
        form.setTitle("Different title");
        form.setCompany("Different company");
        form.setSourceUrl("https://jobs.example.com/role/42");

        JobService service = new JobService(jobs, analysis);

        assertThrows(IllegalArgumentException.class, () -> service.save(user, null, form));
        verify(jobs, never()).save(any(Job.class));
        verifyNoInteractions(analysis);
    }
}
