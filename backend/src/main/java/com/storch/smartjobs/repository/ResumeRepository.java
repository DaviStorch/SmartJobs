package com.storch.smartjobs.repository;

import com.storch.smartjobs.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findAllByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Resume> findByIdAndUserId(Long id, Long userId);
    Optional<Resume> findFirstByUserIdAndActiveTrue(Long userId);
    long countByUserId(Long userId);
}
