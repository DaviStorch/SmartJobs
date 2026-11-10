package com.storch.smartjobs.repository;

import com.storch.smartjobs.entity.GmailImportedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailImportedMessageRepository extends JpaRepository<GmailImportedMessage, Long> {
    boolean existsByUserIdAndMessageId(Long userId, String messageId);
}
