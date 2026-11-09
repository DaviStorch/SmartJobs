package com.storch.smartjobs.controller;

import com.storch.smartjobs.service.CurrentUserService;
import com.storch.smartjobs.service.StatisticsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatisticsController {
    private final CurrentUserService currentUser;
    private final StatisticsService statistics;

    public StatisticsController(CurrentUserService currentUser, StatisticsService statistics) {
        this.currentUser = currentUser;
        this.statistics = statistics;
    }

    @GetMapping("/statistics")
    public String statistics(Authentication authentication, Model model) {
        var user = currentUser.get(authentication);
        model.addAttribute("account", user);
        model.addAttribute("stats", statistics.statistics(user));
        return "statistics";
    }
}
