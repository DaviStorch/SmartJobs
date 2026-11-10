package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.repository.JobRepository;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class StatisticsService {
    private final JobRepository jobRepository;

    public StatisticsService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Map<String, Object> statistics(AppUser user) {
        List<Job> jobs = jobRepository.findAllByUserId(user.getId());
        long applied = jobs.stream().filter(job -> job.getStatus().name().equals("CANDIDATADA")).count();
        Map<String, Long> requirementCounts = new HashMap<>();
        jobs.stream().map(Job::getRequirements).filter(Objects::nonNull)
                .flatMap(requirements -> Arrays.stream(requirements.split("[,;\\n\\r]+")))
                .map(String::strip).filter(value -> value.length() > 1)
                .map(StatisticsService::normalize)
                .forEach(requirement -> requirementCounts.merge(requirement, 1L, Long::sum));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", jobs.size());
        result.put("applied", applied);
        result.put("applicationRate", jobs.isEmpty() ? 0 : Math.round((double) applied * 1000 / jobs.size()) / 10.0);
        result.put("statusCounts", jobRepository.countByStatus(user.getId()));
        result.put("topCompanies", jobRepository.topCompanies(user.getId(), org.springframework.data.domain.PageRequest.of(0, 8)));
        result.put("topSources", jobRepository.topSources(user.getId(), org.springframework.data.domain.PageRequest.of(0, 8)));
        result.put("averageScore", jobRepository.averageScore(user.getId()));
        result.put("requirementCounts", requirementCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(8)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList());
        Locale locale = "en".equals(user.getLanguage()) ? Locale.US : Locale.forLanguageTag("pt-BR");
        DateTimeFormatter monthLabel = DateTimeFormatter.ofPattern("MMM yyyy", locale);
        Map<YearMonth, Long> monthlyCounts = jobs.stream()
                .filter(job -> job.getAppliedAt() != null)
                .collect(Collectors.groupingBy(job -> YearMonth.from(job.getAppliedAt().atZone(ZoneOffset.UTC)),
                        TreeMap::new, Collectors.counting()));
        Map<String, Long> monthlyApplications = new LinkedHashMap<>();
        monthlyCounts.forEach((month, count) -> monthlyApplications.put(month.format(monthLabel), count));
        result.put("monthlyApplications", monthlyApplications);
        return result;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
