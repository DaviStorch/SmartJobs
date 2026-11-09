package com.storch.smartjobs.service;

import com.storch.smartjobs.dto.JobForm;
import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.entity.JobStatus;
import com.storch.smartjobs.repository.JobRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final AnalysisService analysisService;

    public JobService(JobRepository jobRepository, AnalysisService analysisService) {
        this.jobRepository = jobRepository;
        this.analysisService = analysisService;
    }

    @Transactional(readOnly = true)
    public Page<Job> search(AppUser user, String search, String status, String source, String sort, int page) {
        JobStatus parsedStatus = status == null || status.isBlank() ? null : JobStatus.valueOf(status);
        Sort ordering = switch (sort == null ? "recent" : sort) {
            case "score" -> Sort.by(Sort.Direction.DESC, "matchScore").and(Sort.by("createdAt").descending());
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(Math.max(0, page), 10, ordering);
        String normalizedSearch = blankToNull(search);
        String searchPattern = normalizedSearch == null ? null
                : "%" + normalizedSearch.toLowerCase(java.util.Locale.ROOT) + "%";
        String normalizedSource = blankToNull(source);
        return jobRepository.search(user.getId(), searchPattern, parsedStatus,
                normalizedSource == null ? null : normalizedSource.toLowerCase(java.util.Locale.ROOT), pageable);
    }

    @Transactional
    public Job save(AppUser user, Long id, JobForm form) {
        Job job = id == null ? new Job() : ownedJob(id, user);
        rejectDuplicate(user, id, form);
        job.setUser(user);
        job.setTitle(form.getTitle().trim());
        job.setCompany(form.getCompany().trim());
        String description = form.getDescription();
        if ((description == null || description.isBlank()) && form.getPastedText() != null) {
            description = form.getPastedText();
        }
        job.setDescription(valueOrEmpty(description));
        job.setLocation(blankToNull(form.getLocation()));
        job.setWorkMode(blankToNull(form.getWorkMode()));
        job.setSalary(blankToNull(form.getSalary()));
        job.setContractType(blankToNull(form.getContractType()));
        job.setExperienceLevel(blankToNull(form.getExperienceLevel()));
        job.setRequirements(blankToNull(form.getRequirements()));
        job.setBenefits(blankToNull(form.getBenefits()));
        job.setPublishedAt(form.getPublishedAt());
        job.setSource(blankToNull(form.getSource()));
        job.setSourceUrl(blankToNull(form.getSourceUrl()));
        try {
            job.setStatus(JobStatus.valueOf(form.getStatus()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Selecione um status válido.");
        }
        if (job.getStatus() == JobStatus.CANDIDATADA && job.getAppliedAt() == null) {
            job.setAppliedAt(java.time.Instant.now());
        } else if (job.getStatus() != JobStatus.CANDIDATADA) {
            job.setAppliedAt(null);
        }
        job.setNotes(blankToNull(form.getNotes()));
        Job saved = jobRepository.save(job);
        JobAnalysis analysis = analysisService.analyze(saved);
        saved.setMatchScore(analysis.score());
        saved.setRecommendation(analysis.recommendation());
        return jobRepository.save(saved);
    }

    @Transactional(readOnly = true)
    public Job ownedJob(Long id, AppUser user) {
        return jobRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Vaga não encontrada."));
    }

    @Transactional
    public void delete(Long id, AppUser user) {
        jobRepository.delete(ownedJob(id, user));
    }

    @Transactional
    public JobAnalysis analyze(Long id, AppUser user) {
        JobAnalysis analysis = analysisService.analyze(ownedJob(id, user));
        return analysis;
    }

    @Transactional
    public void updateStatus(Long id, AppUser user, String status) {
        Job job = ownedJob(id, user);
        try {
            job.setStatus(JobStatus.valueOf(status));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Selecione um status válido.");
        }
        if (job.getStatus() == JobStatus.CANDIDATADA && job.getAppliedAt() == null) {
            job.setAppliedAt(java.time.Instant.now());
        } else if (job.getStatus() != JobStatus.CANDIDATADA) {
            job.setAppliedAt(null);
        }
        jobRepository.save(job);
    }

    @Transactional
    public void updateNotes(Long id, AppUser user, String notes) {
        if (notes != null && notes.length() > 10000) {
            throw new IllegalArgumentException("As anotações devem ter no máximo 10.000 caracteres.");
        }
        Job job = ownedJob(id, user);
        job.setNotes(blankToNull(notes));
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<Job> recent(AppUser user) {
        return jobRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<Job> topMatches(AppUser user) {
        return jobRepository.findTop5ByUserIdAndMatchScoreIsNotNullOrderByMatchScoreDesc(user.getId());
    }

    @Transactional
    public void recalculateUser(AppUser user) {
        List<Job> jobs = jobRepository.findAllByUserId(user.getId());
        for (Job job : jobs) {
            JobAnalysis analysis = analysisService.analyze(job);
            job.setMatchScore(analysis.score());
            job.setRecommendation(analysis.recommendation());
        }
        jobRepository.saveAll(jobs);
    }

    private void rejectDuplicate(AppUser user, Long currentId, JobForm form) {
        List<Job> candidates = jobRepository.findAllByUserId(user.getId());
        for (Job candidate : candidates) {
            if (!candidate.getId().equals(currentId) && JobDuplicateChecker.equivalent(
                    candidate.getSourceUrl(), form.getSourceUrl(), candidate.getCompany(), form.getCompany(),
                    candidate.getTitle(), form.getTitle())) {
                throw new IllegalArgumentException("Esta oportunidade já está cadastrada ("
                        + candidate.getTitle() + " — " + candidate.getCompany() + ").");
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
