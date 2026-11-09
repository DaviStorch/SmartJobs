package com.storch.smartjobs.entity;

public enum ResumeSectionType {
    PERSONAL_INFO("Informações pessoais", "Personal information"),
    PROFESSIONAL_SUMMARY("Resumo profissional", "Professional summary"),
    OBJECTIVES("Objetivos", "Objectives"),
    EDUCATION("Formação acadêmica", "Education"),
    EXPERIENCE("Experiências profissionais", "Work experience"),
    PROJECTS("Projetos", "Projects"),
    TECHNICAL_SKILLS("Habilidades técnicas", "Technical skills"),
    LANGUAGES("Idiomas", "Languages"),
    CERTIFICATIONS("Certificações", "Certifications"),
    IMPORTED_TEXT("Texto extraído do PDF", "Text extracted from PDF");

    private final String portugueseLabel;
    private final String englishLabel;

    ResumeSectionType(String portugueseLabel, String englishLabel) {
        this.portugueseLabel = portugueseLabel;
        this.englishLabel = englishLabel;
    }

    public String label(String language) {
        return "en".equals(language) ? englishLabel : portugueseLabel;
    }
}
