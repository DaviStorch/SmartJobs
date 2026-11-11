package com.storch.smartjobs.repository;

import com.storch.smartjobs.entity.GmailConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<GmailConnection> findByUserId(Long userId);
    void deleteByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<GmailConnection> findAll();
}
