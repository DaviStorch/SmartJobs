package com.storch.smartjobs.controller;

import com.storch.smartjobs.dto.JobForm;
import com.storch.smartjobs.entity.JobStatus;
import com.storch.smartjobs.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jobs")
public class JobController {
    private final CurrentUserService currentUser;
    private final JobService jobService;
    private final JobTextParser textParser;

    public JobController(CurrentUserService currentUser, JobService jobService, JobTextParser textParser) {
        this.currentUser = currentUser;
        this.jobService = jobService;
        this.textParser = textParser;
    }

    @GetMapping
    public String list(Authentication authentication, Model model,
                       @RequestParam(required = false) String q, @RequestParam(required = false) String status,
                       @RequestParam(required = false) String source, @RequestParam(defaultValue = "recent") String sort,
                       @RequestParam(defaultValue = "0") int page) {
        var user = currentUser.get(authentication);
        model.addAttribute("jobs", jobService.search(user, q, status, source, sort, page));
        model.addAttribute("statuses", JobStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("source", source);
        model.addAttribute("sort", sort);
        return "jobs/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new JobForm());
        model.addAttribute("statuses", JobStatus.values());
        model.addAttribute("jobId", null);
        return "jobs/form";
    }

    @PostMapping("/import")
    public String importText(@RequestParam String pastedText, Model model) {
        try {
            model.addAttribute("form", textParser.parse(pastedText));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("form", new JobForm());
            model.addAttribute("error", exception.getMessage());
        }
        model.addAttribute("statuses", JobStatus.values());
        model.addAttribute("jobId", null);
        return "jobs/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("form", JobForm.from(jobService.ownedJob(id, currentUser.get(authentication))));
        model.addAttribute("statuses", JobStatus.values());
        model.addAttribute("jobId", id);
        return "jobs/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") JobForm form, BindingResult result,
                         Authentication authentication, Model model, RedirectAttributes redirect) {
        return save(null, form, result, authentication, model, redirect);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") JobForm form, BindingResult result,
                         Authentication authentication, Model model, RedirectAttributes redirect) {
        return save(id, form, result, authentication, model, redirect);
    }

    private String save(Long id, JobForm form, BindingResult result, Authentication authentication,
                        Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", JobStatus.values());
            model.addAttribute("jobId", id);
            return "jobs/form";
        }
        try {
            var job = jobService.save(currentUser.get(authentication), id, form);
            redirect.addFlashAttribute("success", "Oportunidade salva com sucesso.");
            return "redirect:/jobs/" + job.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("statuses", JobStatus.values());
            model.addAttribute("jobId", id);
            return "jobs/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        var user = currentUser.get(authentication);
        model.addAttribute("job", jobService.ownedJob(id, user));
        model.addAttribute("analysis", jobService.analyze(id, user));
        model.addAttribute("statuses", JobStatus.values());
        return "jobs/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status,
                               Authentication authentication, RedirectAttributes redirect) {
        jobService.updateStatus(id, currentUser.get(authentication), status);
        redirect.addFlashAttribute("success", "Status atualizado.");
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/notes")
    public String updateNotes(@PathVariable Long id, @RequestParam String notes,
                              Authentication authentication, RedirectAttributes redirect) {
        jobService.updateNotes(id, currentUser.get(authentication), notes);
        redirect.addFlashAttribute("success", "Anotações salvas.");
        return "redirect:/jobs/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        jobService.delete(id, currentUser.get(authentication));
        redirect.addFlashAttribute("success", "Oportunidade removida.");
        return "redirect:/jobs";
    }
}
