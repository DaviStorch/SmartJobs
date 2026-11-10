package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.ResumeSectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeImportParserTest {
    private final ResumeImportParser parser = new ResumeImportParser();

    @Test
    void mapsSkillsFromTechnicalSectionAndKeepsUnclassifiedSectionsEmpty() {
        var draft = parser.parse("""
                EXPERIÊNCIA PROFISSIONAL
                Desenvolvi APIs com Java e Spring Boot.
                HABILIDADES TÉCNICAS
                PostgreSQL, Docker, Git
                FORMAÇÃO ACADÊMICA
                Universidade Exemplo — Ciência da Computação
                """, null);

        assertTrue(draft.sections().get(ResumeSectionType.TECHNICAL_SKILLS.name()).contains("PostgreSQL"));
        assertTrue(draft.sections().get(ResumeSectionType.TECHNICAL_SKILLS.name()).contains("Docker"));
        assertTrue(draft.sections().get(ResumeSectionType.EXPERIENCE.name()).contains("Desenvolvi APIs"));
        assertTrue(draft.sections().get(ResumeSectionType.EDUCATION.name()).contains("Universidade Exemplo"));
        assertTrue(draft.sections().get(ResumeSectionType.LANGUAGES.name()).isEmpty());
    }

    @Test
    void extractsContactsAndRetainsRawTextWithoutInventingOtherFields() {
        var draft = parser.parse("Nome Exemplo\nname@example.com\nhttps://github.com/example\n", null);

        assertTrue(draft.sections().get(ResumeSectionType.PERSONAL_INFO.name()).contains("name@example.com"));
        assertTrue(draft.sections().get(ResumeSectionType.PERSONAL_INFO.name()).contains("github.com/example"));
        assertTrue(draft.sections().get(ResumeSectionType.IMPORTED_TEXT.name()).contains("Nome Exemplo"));
        assertTrue(draft.sections().get(ResumeSectionType.EDUCATION.name()).isEmpty());
    }
}
