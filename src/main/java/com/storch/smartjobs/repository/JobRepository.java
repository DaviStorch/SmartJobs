package com.storch.smartjobs.repository;

import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    @Query("""
            select j from Job j
            where j.user.id = :userId
              and (:searchPattern is null or lower(j.title) like :searchPattern
                   or lower(j.company) like :searchPattern)
              and (:status is null or j.status = :status)
              and (:source is null or lower(j.source) = :source)
            """)
    Page<Job> search(@Param("userId") Long userId, @Param("searchPattern") String searchPattern,
                     @Param("status") JobStatus status, @Param("source") String source, Pageable pageable);

    Optional<Job> findByIdAndUserId(Long id, Long userId);
    List<Job> findAllByUserId(Long userId);
    List<Job> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    List<Job> findTop5ByUserIdAndMatchScoreIsNotNullOrderByMatchScoreDesc(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndStatus(Long userId, JobStatus status);
    @Query("select avg(j.matchScore) from Job j where j.user.id = :userId and j.matchScore is not null")
    Double averageScore(@Param("userId") Long userId);
    @Query("select j.status, count(j) from Job j where j.user.id = :userId group by j.status")
    List<Object[]> countByStatus(@Param("userId") Long userId);
    @Query("select j.company, count(j) from Job j where j.user.id = :userId group by j.company order by count(j) desc")
    List<Object[]> topCompanies(@Param("userId") Long userId, Pageable pageable);
    @Query("select j.source, count(j) from Job j where j.user.id = :userId and j.source is not null group by j.source order by count(j) desc")
    List<Object[]> topSources(@Param("userId") Long userId, Pageable pageable);
}
