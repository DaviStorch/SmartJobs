package com.storch.smartjobs.controller;

import com.storch.smartjobs.service.CurrentUserService;
import com.storch.smartjobs.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final CurrentUserService currentUser;
    private final DashboardService dashboard;

    public DashboardController(CurrentUserService currentUser, DashboardService dashboard) {
        this.currentUser = currentUser;
        this.dashboard = dashboard;
    }

    @GetMapping("/")
    public String dashboard(Authentication authentication, Model model) {
        var user = currentUser.get(authentication);
        model.addAttribute("account", user);
        model.addAttribute("stats", dashboard.summary(user));
        return "dashboard";
    }
}
