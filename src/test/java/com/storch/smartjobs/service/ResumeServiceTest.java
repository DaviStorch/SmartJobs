package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.entity.Resume;
import com.storch.smartjobs.entity.ResumeSection;
import com.storch.smartjobs.entity.ResumeSectionType;
import com.storch.smartjobs.repository.ResumeRepository;
import com.storch.smartjobs.repository.ResumeSectionRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResumeServiceTest {
    @Test
    void extractsSelectablePdfTextWithoutGuessingOtherSections() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(30, 750);
                content.showText("Java and PostgreSQL experience");
                content.endText();
            }
            document.save(bytes);
        }

        ResumeRepository resumes = mock(ResumeRepository.class);
        ResumeSectionRepository sections = mock(ResumeSectionRepository.class);
        when(resumes.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resumes.countByUserId(any())).thenReturn(1L);
        when(sections.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ResumeService service = new ResumeService(resumes, sections, mock(JobService.class));
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", bytes.toByteArray());

        Resume result = service.importPdf(mock(AppUser.class), "Technical profile", file);

        assertEquals("Technical profile", result.getName());
        verify(sections, atLeastOnce()).save(argThat(section ->
                section.getType() == ResumeSectionType.IMPORTED_TEXT
                        && section.getContent().contains("Java and PostgreSQL experience")));
    }

    @Test
    void rejectsFilesThatAreNotPdfsBeforeSaving() {
        ResumeRepository resumes = mock(ResumeRepository.class);
        ResumeSectionRepository sections = mock(ResumeSectionRepository.class);
        ResumeService service = new ResumeService(resumes, sections, mock(JobService.class));
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "not a pdf".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.importPdf(mock(AppUser.class), "Resume", file));
        verifyNoInteractions(resumes, sections);
    }
}
