package com.storch.smartjobs.controller;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.repository.UserRepository;
import com.storch.smartjobs.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

@Controller
public class PreferencesController {
    private final CurrentUserService currentUser;
    private final UserRepository userRepository;
    private final LocaleResolver localeResolver;

    public PreferencesController(CurrentUserService currentUser, UserRepository userRepository, LocaleResolver localeResolver) {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        this.localeResolver = localeResolver;
    }

    @PostMapping("/preferences")
    public String update(Authentication authentication, @RequestParam(defaultValue = "light") String theme,
                         @RequestParam(defaultValue = "pt") String language,
                         HttpServletRequest request, HttpServletResponse response) {
        if (!theme.equals("light") && !theme.equals("dark")) {
            throw new IllegalArgumentException("Tema inválido.");
        }
        if (!language.equals("pt") && !language.equals("en")) {
            throw new IllegalArgumentException("Idioma inválido.");
        }
        AppUser user = currentUser.get(authentication);
        user.setTheme(theme);
        user.setLanguage(language);
        userRepository.save(user);
        localeResolver.setLocale(request, response, java.util.Locale.forLanguageTag(language.equals("en") ? "en" : "pt-BR"));
        return "redirect:" + safeReferer(request.getHeader("Referer"));
    }

    private String safeReferer(String referer) {
        if (referer == null) return "/";
        try {
            java.net.URI uri = java.net.URI.create(referer);
            return uri.getHost() == null ? "/" : uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
        } catch (IllegalArgumentException exception) {
            return "/";
        }
    }
}
