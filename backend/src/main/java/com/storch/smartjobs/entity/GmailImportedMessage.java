package com.storch.smartjobs.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gmail_imported_message", uniqueConstraints = @UniqueConstraint(
        name = "uq_gmail_message_user_message", columnNames = {"user_id", "message_id"}))
public class GmailImportedMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "message_id", nullable = false, length = 200)
    private String messageId;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected GmailImportedMessage() {
    }

    public GmailImportedMessage(AppUser user, String messageId, String result) {
        this.user = user;
        this.messageId = messageId;
        this.result = result;
    }
}
