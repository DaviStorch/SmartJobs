package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.entity.Resume;
import com.storch.smartjobs.entity.ResumeSection;
import com.storch.smartjobs.repository.ResumeRepository;
import com.storch.smartjobs.repository.ResumeSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnalysisServiceTest {
    private ResumeRepository resumeRepository;
    private ResumeSectionRepository sectionRepository;
    private AnalysisService service;
    private Job job;

    @BeforeEach
    void setUp() {
        resumeRepository = mock(ResumeRepository.class);
        sectionRepository = mock(ResumeSectionRepository.class);
        service = new AnalysisService(resumeRepository, sectionRepository);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(7L);
        job = new Job();
        job.setUser(user);
    }

    @Test
    void scoresMatchedPartialAndMissingRequirementsWithAnExplanation() {
        Resume resume = mock(Resume.class);
        when(resume.getId()).thenReturn(10L);
        when(resumeRepository.findFirstByUserIdAndActiveTrue(7L)).thenReturn(Optional.of(resume));
        ResumeSection section = mock(ResumeSection.class);
        when(section.getContent()).thenReturn("Experiência com Java e SQL em projetos de produto.");
        when(sectionRepository.findAllByResumeIdOrderByType(10L)).thenReturn(List.of(section));
        job.setRequirements("Java, SQL, Kubernetes");

        JobAnalysis result = service.analyze(job);

        assertEquals(3, result.requirements().size());
        assertEquals(3.33, result.score().doubleValue(), 0.01);
        assertEquals("CONSIDERAR", result.recommendation().name());
        assertEquals(2, result.positives().size());
        assertEquals(List.of("Kubernetes"), result.negatives());
        assertTrue(result.rationale().contains("determinística"));
    }

    @Test
    void returnsNoScoreWhenThereIsNoActiveResume() {
        when(resumeRepository.findFirstByUserIdAndActiveTrue(any())).thenReturn(Optional.empty());

        JobAnalysis result = service.analyze(job);

        assertNull(result.score());
        assertFalse(result.resumeAvailable());
        assertTrue(result.suggestions().getFirst().contains("currículo"));
    }

    @Test
    void doesNotMatchAShortSkillInsideAnUnrelatedWord() {
        Resume resume = mock(Resume.class);
        when(resume.getId()).thenReturn(10L);
        when(resumeRepository.findFirstByUserIdAndActiveTrue(7L)).thenReturn(Optional.of(resume));
        ResumeSection section = mock(ResumeSection.class);
        when(section.getContent()).thenReturn("I worked with Google Analytics.");
        when(sectionRepository.findAllByResumeIdOrderByType(10L)).thenReturn(List.of(section));
        job.setRequirements("Go");

        JobAnalysis result = service.analyze(job);

        assertEquals(0.0, result.score().doubleValue());
        assertEquals(RequirementMatch.Status.MISSING, result.requirements().getFirst().status());
    }

    @Test
    void leavesScoreUnsetWhenTheOpportunityHasNoRequirements() {
        Resume resume = mock(Resume.class);
        when(resume.getId()).thenReturn(10L);
        when(resumeRepository.findFirstByUserIdAndActiveTrue(7L)).thenReturn(Optional.of(resume));
        ResumeSection section = mock(ResumeSection.class);
        when(section.getContent()).thenReturn("Java");
        when(sectionRepository.findAllByResumeIdOrderByType(10L)).thenReturn(List.of(section));

        JobAnalysis result = service.analyze(job);

        assertNull(result.score());
        assertEquals("CONSIDERAR", result.recommendation().name());
    }
}
