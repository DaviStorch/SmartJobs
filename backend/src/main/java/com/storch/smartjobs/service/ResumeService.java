package com.storch.smartjobs.service;

import com.storch.smartjobs.entity.*;
import com.storch.smartjobs.repository.ResumeRepository;
import com.storch.smartjobs.repository.ResumeSectionRepository;
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
    private final PdfOcrService pdfOcrService;
    private final ResumeImportParser importParser;

    public ResumeService(ResumeRepository resumeRepository, ResumeSectionRepository sectionRepository,
                         JobService jobService, PdfOcrService pdfOcrService, ResumeImportParser importParser) {
        this.resumeRepository = resumeRepository;
        this.sectionRepository = sectionRepository;
        this.jobService = jobService;
        this.pdfOcrService = pdfOcrService;
        this.importParser = importParser;
    }

    @Transactional(readOnly = true)
    public List<Resume> all(AppUser user) {
        return resumeRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    @Transactional
    public Resume create(AppUser user, String name, String importedText) {
        Map<String, String> sections = new HashMap<>();
        for (ResumeSectionType type : ResumeSectionType.values()) {
            sections.put(type.name(), type == ResumeSectionType.IMPORTED_TEXT ? Objects.requireNonNullElse(importedText, "") : "");
        }
        return create(user, name, sections);
    }

    @Transactional
    public Resume create(AppUser user, String name, Map<String, String> contents) {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("Informe um nome de currículo com até 120 caracteres.");
        }
        Map<ResumeSectionType, String> validated = validateContents(contents);
        Resume resume = resumeRepository.save(new Resume(user, name.trim()));
        for (ResumeSectionType type : ResumeSectionType.values()) {
            sectionRepository.save(new ResumeSection(resume, type, validated.getOrDefault(type, "")));
        }
        if (resumeRepository.countByUserId(user.getId()) == 1) {
            resume.setActive(true);
            resumeRepository.save(resume);
            jobService.recalculateUser(user);
        }
        return resume;
    }

    @Transactional
    public ResumeImportDraft previewPdf(MultipartFile file) {
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
            PdfTextExtractor extracted = pdfOcrService.extract(bytes);
            String warning = extracted.warning();
            if (extracted.ocrUsed() && warning == null) {
                warning = "O documento foi lido com OCR. Revise cuidadosamente os dados reconhecidos antes de salvar.";
            }
            return importParser.parse(extracted.text(), warning);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
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

    @Transactional(readOnly = true)
    public List<ResumeSection> activeSections(AppUser user) {
        return resumeRepository.findFirstByUserIdAndActiveTrue(user.getId())
                .map(resume -> sectionRepository.findAllByResumeIdOrderByType(resume.getId()))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveResume(AppUser user) {
        return resumeRepository.findFirstByUserIdAndActiveTrue(user.getId()).isPresent();
    }

    @Transactional
    public void saveSection(Long id, AppUser user, String sectionName, String content) {
        saveSections(id, user, Map.of(sectionName, content));
    }

    @Transactional
    public void saveSections(Long id, AppUser user, Map<String, String> contents) {
        Map<ResumeSectionType, String> validated = validateContents(contents);
        Resume resume = ownedResume(id, user);
        List<ResumeSection> sections = sectionRepository.findAllByResumeIdOrderByType(id);
        Map<ResumeSectionType, ResumeSection> byType = new EnumMap<>(ResumeSectionType.class);
        sections.forEach(section -> byType.put(section.getType(), section));
        for (Map.Entry<ResumeSectionType, String> entry : validated.entrySet()) {
            ResumeSection section = byType.get(entry.getKey());
            if (section == null) section = new ResumeSection(resume, entry.getKey(), "");
            section.setContent(entry.getValue());
            sectionRepository.save(section);
        }
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

    private Map<ResumeSectionType, String> validateContents(Map<String, String> contents) {
        if (contents == null) throw new IllegalArgumentException("Nenhum conteúdo de currículo foi enviado.");
        Map<ResumeSectionType, String> validated = new EnumMap<>(ResumeSectionType.class);
        contents.forEach((key, content) -> {
            if (key == null || key.length() > 80) throw new IllegalArgumentException("Seção de currículo inválida.");
            ResumeSectionType type;
            try {
                type = ResumeSectionType.valueOf(key);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Seção de currículo inválida: " + key, exception);
            }
            if (content == null || content.length() > 30000) {
                throw new IllegalArgumentException("Cada módulo deve ter no máximo 30.000 caracteres.");
            }
            validated.put(type, content);
        });
        return validated;
    }
}
