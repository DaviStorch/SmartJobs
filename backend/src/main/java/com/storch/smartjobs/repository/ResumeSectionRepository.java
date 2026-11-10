package com.storch.smartjobs.repository;

import com.storch.smartjobs.entity.ResumeSection;
import com.storch.smartjobs.entity.ResumeSectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResumeSectionRepository extends JpaRepository<ResumeSection, Long> {
    List<ResumeSection> findAllByResumeIdOrderByType(Long resumeId);
    Optional<ResumeSection> findByResumeIdAndType(Long resumeId, ResumeSectionType type);
    void deleteAllByResumeId(Long resumeId);
}
