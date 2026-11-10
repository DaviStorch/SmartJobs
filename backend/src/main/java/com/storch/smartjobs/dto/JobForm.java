package com.storch.smartjobs.dto;

import com.storch.smartjobs.entity.Job;
import com.storch.smartjobs.entity.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class JobForm {
    @NotBlank @Size(max = 180)
    private String title;
    @NotBlank @Size(max = 160)
    private String company;
    @Size(max = 20000)
    private String description;
    @Size(max = 160)
    private String location;
    @Size(max = 30)
    private String workMode;
    @Size(max = 120)
    private String salary;
    @Size(max = 80)
    private String contractType;
    @Size(max = 80)
    private String experienceLevel;
    @Size(max = 10000)
    private String requirements;
    @Size(max = 10000)
    private String benefits;
    private LocalDate publishedAt;
    @Size(max = 100)
    private String source;
    @Size(max = 1000)
    @Pattern(regexp = "^$|^https?://[^\\s]+$", message = "Informe um link HTTP/HTTPS válido.")
    private String sourceUrl;
    private String status = JobStatus.PENDENTE.name();
    @Size(max = 10000)
    private String notes;
    @Size(max = 30000)
    private String pastedText;

    public static JobForm from(Job job) {
        JobForm form = new JobForm();
        form.title = job.getTitle();
        form.company = job.getCompany();
        form.description = job.getDescription();
        form.location = job.getLocation();
        form.workMode = job.getWorkMode();
        form.salary = job.getSalary();
        form.contractType = job.getContractType();
        form.experienceLevel = job.getExperienceLevel();
        form.requirements = job.getRequirements();
        form.benefits = job.getBenefits();
        form.publishedAt = job.getPublishedAt();
        form.source = job.getSource();
        form.sourceUrl = job.getSourceUrl();
        form.status = job.getStatus().name();
        form.notes = job.getNotes();
        return form;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
    public LocalDate getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDate publishedAt) { this.publishedAt = publishedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getPastedText() { return pastedText; }
    public void setPastedText(String pastedText) { this.pastedText = pastedText; }
}
