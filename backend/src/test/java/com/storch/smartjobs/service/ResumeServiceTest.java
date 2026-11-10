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
import static org.mockito.ArgumentMatchers.argThat;
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
        ResumeService service = new ResumeService(resumes, sections, mock(JobService.class),
                new PdfOcrService("", "por+eng", 20), new ResumeImportParser());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", bytes.toByteArray());

        ResumeImportDraft result = service.previewPdf(file);

        assertTrue(result.sections().get(ResumeSectionType.IMPORTED_TEXT.name()).contains("Java and PostgreSQL experience"));
        assertTrue(result.sections().get(ResumeSectionType.TECHNICAL_SKILLS.name()).contains("Java"));
        assertTrue(result.sections().get(ResumeSectionType.TECHNICAL_SKILLS.name()).contains("PostgreSQL"));
        verifyNoInteractions(resumes, sections);
    }

    @Test
    void rejectsFilesThatAreNotPdfsBeforeSaving() {
        ResumeRepository resumes = mock(ResumeRepository.class);
        ResumeSectionRepository sections = mock(ResumeSectionRepository.class);
        ResumeService service = new ResumeService(resumes, sections, mock(JobService.class),
                new PdfOcrService("", "por+eng", 20), new ResumeImportParser());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "not a pdf".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.previewPdf(file));
        verifyNoInteractions(resumes, sections);
    }

    @Test
    void scannedPdfReturnsExplicitWarningWhenOcrIsNotConfigured() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(bytes);
        }
        ResumeService service = new ResumeService(mock(ResumeRepository.class), mock(ResumeSectionRepository.class),
                mock(JobService.class), new PdfOcrService("", "por+eng", 20), new ResumeImportParser());
        var draft = service.previewPdf(new MockMultipartFile("file", "scan.pdf", "application/pdf", bytes.toByteArray()));
        assertNotNull(draft.warning());
        assertTrue(draft.warning().contains("OCR não está configurado"));
    }

    @Test
    void savesSeveralModulesInOneServiceOperation() {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(4L);
        Resume resume = new Resume(user, "Profile");
        ResumeRepository resumes = mock(ResumeRepository.class);
        ResumeSectionRepository sections = mock(ResumeSectionRepository.class);
        when(resumes.findByIdAndUserId(12L, 4L)).thenReturn(java.util.Optional.of(resume));
        when(sections.findAllByResumeIdOrderByType(12L)).thenReturn(java.util.List.of());
        when(sections.save(any(ResumeSection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ResumeService service = new ResumeService(resumes, sections, mock(JobService.class),
                new PdfOcrService("", "por+eng", 20), new ResumeImportParser());

        service.saveSections(12L, user, java.util.Map.of(
                "PERSONAL_INFO", "person@example.com",
                "TECHNICAL_SKILLS", "Java, PostgreSQL"));

        verify(sections, times(2)).save(argThat(section ->
                section.getType() == ResumeSectionType.PERSONAL_INFO
                        || section.getType() == ResumeSectionType.TECHNICAL_SKILLS));
    }

    @Test
    void validatesEveryModuleBeforePersistingAnySection() {
        ResumeRepository resumes = mock(ResumeRepository.class);
        ResumeSectionRepository sections = mock(ResumeSectionRepository.class);
        ResumeService service = new ResumeService(resumes, sections, mock(JobService.class),
                new PdfOcrService("", "por+eng", 20), new ResumeImportParser());

        assertThrows(IllegalArgumentException.class, () -> service.saveSections(12L, mock(AppUser.class),
                java.util.Map.of("TECHNICAL_SKILLS", "x".repeat(30001))));

        verifyNoInteractions(resumes, sections);
    }
}
