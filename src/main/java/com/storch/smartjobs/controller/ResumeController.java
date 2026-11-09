package com.storch.smartjobs.controller;

import com.storch.smartjobs.entity.ResumeSectionType;
import com.storch.smartjobs.service.CurrentUserService;
import com.storch.smartjobs.service.ResumeService;
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
    public String importPdf(@RequestParam String name, @RequestParam MultipartFile file, Authentication authentication,
                            RedirectAttributes redirect) {
        var resume = resumeService.importPdf(currentUser.get(authentication), name, file);
        redirect.addFlashAttribute("success", "Texto extraído. Revise e edite os módulos: a extração de PDF pode não preservar toda a formatação.");
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

    @PostMapping("/{id}/section/{type}")
    public String saveSection(@PathVariable Long id, @PathVariable String type, @RequestParam String content,
                              Authentication authentication, RedirectAttributes redirect) {
        resumeService.saveSection(id, currentUser.get(authentication), type, content);
        redirect.addFlashAttribute("success", "Módulo salvo.");
        return "redirect:/resumes/" + id;
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
