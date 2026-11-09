package com.storch.smartjobs.service;

import com.storch.smartjobs.dto.JobForm;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JobTextParserTest {
    private final JobTextParser parser = new JobTextParser();

    @Test
    void keepsOriginalTextAndOnlyUsesExplicitCompanyLabel() {
        String text = "Senior Engineer\nCompany: Example Ltd.\nRequirements: Java, PostgreSQL";
        JobForm form = parser.parse(text);
        assertEquals("Senior Engineer", form.getTitle());
        assertEquals("Example Ltd.", form.getCompany());
        assertEquals(text, form.getDescription());
    }

    @Test
    void rejectsBlankOrOversizedInput() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("x".repeat(30001)));
    }
}
