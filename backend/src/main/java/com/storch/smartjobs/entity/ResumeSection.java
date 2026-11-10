package com.storch.smartjobs.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "resume_section",
        uniqueConstraints = @UniqueConstraint(name = "uq_resume_section", columnNames = {"resume_id", "section_type"}))
public class ResumeSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 30)
    private ResumeSectionType type;

    @Column(nullable = false, columnDefinition = "text")
    private String content = "";

    protected ResumeSection() {
    }

    public ResumeSection(Resume resume, ResumeSectionType type, String content) {
        this.resume = resume;
        this.type = type;
        this.content = content;
    }

    public Long getId() { return id; }
    public Resume getResume() { return resume; }
    public ResumeSectionType getType() { return type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
