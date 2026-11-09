package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.*;
import com.storch.smartjobs.repository.ResumeRepository;
import com.storch.smartjobs.repository.ResumeSectionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@Service
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final ResumeSectionRepository sectionRepository;
    private final JobService jobService;

    public ResumeService(ResumeRepository resumeRepository, ResumeSectionRepository sectionRepository, JobService jobService) {
        this.resumeRepository = resumeRepository;
        this.sectionRepository = sectionRepository;
        this.jobService = jobService;
    }

    @Transactional(readOnly = true)
    public List<Resume> all(AppUser user) {
        return resumeRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    @Transactional
    public Resume create(AppUser user, String name, String importedText) {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("Informe um nome de currículo com até 120 caracteres.");
        }
        Resume resume = resumeRepository.save(new Resume(user, name.trim()));
        for (ResumeSectionType type : ResumeSectionType.values()) {
            String content = type == ResumeSectionType.IMPORTED_TEXT ? Objects.requireNonNullElse(importedText, "") : "";
            sectionRepository.save(new ResumeSection(resume, type, content));
        }
        if (resumeRepository.countByUserId(user.getId()) == 1) {
            resume.setActive(true);
            resumeRepository.save(resume);
            jobService.recalculateUser(user);
        }
        return resume;
    }

    @Transactional
    public Resume importPdf(AppUser user, String name, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione um arquivo PDF.");
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf") || file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Envie um arquivo PDF de até 5 MB.");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5 || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
                throw new IllegalArgumentException("O arquivo não parece ser um PDF válido.");
            }
            String text;
            try (PDDocument document = Loader.loadPDF(bytes)) {
                text = new PDFTextStripper().getText(document);
            }
            return create(user, name, text);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível extrair texto deste PDF. Confira se o arquivo não está danificado ou protegido.", exception);
        }
    }

    @Transactional(readOnly = true)
    public Resume ownedResume(Long id, AppUser user) {
        return resumeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Currículo não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<ResumeSection> sections(Long id, AppUser user) {
        ownedResume(id, user);
        return sectionRepository.findAllByResumeIdOrderByType(id);
    }

    @Transactional
    public void saveSection(Long id, AppUser user, String sectionName, String content) {
        if (content == null || content.length() > 30000) {
            throw new IllegalArgumentException("O conteúdo deve ter no máximo 30.000 caracteres.");
        }
        Resume resume = ownedResume(id, user);
        ResumeSectionType type;
        try {
            type = ResumeSectionType.valueOf(sectionName);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Seção de currículo inválida.");
        }
        ResumeSection section = sectionRepository.findByResumeIdAndType(id, type)
                .orElseGet(() -> new ResumeSection(resume, type, ""));
        section.setContent(content);
        sectionRepository.save(section);
        if (resume.isActive()) jobService.recalculateUser(user);
    }

    @Transactional
    public void activate(Long id, AppUser user) {
        Resume selected = ownedResume(id, user);
        resumeRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId()).forEach(resume -> resume.setActive(false));
        selected.setActive(true);
        resumeRepository.flush();
        jobService.recalculateUser(user);
    }

    @Transactional
    public void delete(Long id, AppUser user) {
        Resume resume = ownedResume(id, user);
        boolean wasActive = resume.isActive();
        resumeRepository.delete(resume);
        if (wasActive) {
            resumeRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId()).stream().findFirst().ifPresent(next -> {
                next.setActive(true);
                resumeRepository.save(next);
            });
            jobService.recalculateUser(user);
        }
    }
}
