package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.JobStatus;
import com.storch.smartjobs.repository.JobRepository;
import com.storch.smartjobs.repository.ResumeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DashboardService {
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    public DashboardService(JobRepository jobRepository, ResumeRepository resumeRepository) {
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
    }

    public Map<String, Object> summary(AppUser user) {
        Long userId = user.getId();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("total", jobRepository.countByUserId(userId));
        values.put("pending", jobRepository.countByUserIdAndStatus(userId, JobStatus.PENDENTE));
        values.put("applied", jobRepository.countByUserIdAndStatus(userId, JobStatus.CANDIDATADA));
        values.put("rejected", jobRepository.countByUserIdAndStatus(userId, JobStatus.RECUSADA));
        values.put("dismissed", jobRepository.countByUserIdAndStatus(userId, JobStatus.DISPENSADA));
        values.put("averageScore", jobRepository.averageScore(userId));
        values.put("resumeCount", resumeRepository.countByUserId(userId));
        values.put("recentJobs", jobRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId));
        values.put("topJobs", jobRepository.findTop5ByUserIdAndMatchScoreIsNotNullOrderByMatchScoreDesc(userId));
        values.put("statusCounts", jobRepository.countByStatus(userId));
        values.put("topCompanies", jobRepository.topCompanies(userId, PageRequest.of(0, 5)));
        values.put("topSources", jobRepository.topSources(userId, PageRequest.of(0, 5)));
        return values;
    }
}
