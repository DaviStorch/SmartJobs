package com.storch.smartjobs.repository;

import com.storch.smartjobs.entity.GmailConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, Long> {
    Optional<GmailConnection> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
