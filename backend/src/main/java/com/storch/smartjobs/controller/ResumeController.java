package com.storch.smartjobs.controller;

import com.storch.smartjobs.entity.ResumeSectionType;
import com.storch.smartjobs.service.CurrentUserService;
import com.storch.smartjobs.service.ResumeService;
import org.springframework.util.MultiValueMap;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resumes")
public class ResumeController {
    private final CurrentUserService currentUser;
    private final ResumeService resumeService;

    public ResumeController(CurrentUserService currentUser, ResumeService resumeService) {
        this.currentUser = currentUser;
        this.resumeService = resumeService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("resumes", resumeService.all(currentUser.get(authentication)));
        return "resumes/list";
    }

    @PostMapping
    public String create(@RequestParam String name, Authentication authentication, RedirectAttributes redirect) {
        var resume = resumeService.create(currentUser.get(authentication), name, "");
        redirect.addFlashAttribute("success", "Currículo criado.");
        return "redirect:/resumes/" + resume.getId();
    }

    @PostMapping("/import")
    public String previewImport(@RequestParam String name, @RequestParam MultipartFile file, Model model) {
        model.addAttribute("name", name);
        model.addAttribute("draft", resumeService.previewPdf(file));
        return "resumes/import-preview";
    }

    @PostMapping("/import/save")
    public String saveImport(@RequestParam String name, @RequestParam MultiValueMap<String, String> form,
                             Authentication authentication, RedirectAttributes redirect) {
        var resume = resumeService.create(currentUser.get(authentication), name, sectionValues(form));
        redirect.addFlashAttribute("success", "Importação concluída. Revise os módulos e complete os campos não identificados.");
        return "redirect:/resumes/" + resume.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        var user = currentUser.get(authentication);
        model.addAttribute("resume", resumeService.ownedResume(id, user));
        model.addAttribute("sections", resumeService.sections(id, user));
        model.addAttribute("sectionTypes", ResumeSectionType.values());
        return "resumes/detail";
    }

    @PostMapping("/{id}/save")
    public String saveSections(@PathVariable Long id, @RequestParam MultiValueMap<String, String> form,
                               @RequestParam(required = false) String saveType,
                               Authentication authentication, RedirectAttributes redirect) {
        var values = sectionValues(form);
        if (saveType != null && !saveType.isBlank()) {
            String value = values.get(saveType);
            if (value == null) throw new IllegalArgumentException("O módulo selecionado não foi enviado.");
            resumeService.saveSection(id, currentUser.get(authentication), saveType, value);
            redirect.addFlashAttribute("success", "Módulo salvo.");
        } else {
            resumeService.saveSections(id, currentUser.get(authentication), values);
            redirect.addFlashAttribute("success", "Todos os módulos foram salvos.");
        }
        return "redirect:/resumes/" + id;
    }

    private java.util.Map<String, String> sectionValues(MultiValueMap<String, String> form) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        form.forEach((key, entries) -> {
            if (key.startsWith("section_")) {
                values.put(key.substring("section_".length()), entries == null || entries.isEmpty() ? "" : entries.get(0));
            }
        });
        return values;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        resumeService.activate(id, currentUser.get(authentication));
        redirect.addFlashAttribute("success", "Currículo ativo para as análises.");
        return "redirect:/resumes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        resumeService.delete(id, currentUser.get(authentication));
        redirect.addFlashAttribute("success", "Currículo removido.");
        return "redirect:/resumes";
    }
}
