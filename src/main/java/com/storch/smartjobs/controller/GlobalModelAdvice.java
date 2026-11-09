package com.storch.smartjobs.controller;

import com.storch.smartjobs.repository.UserRepository;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Locale;

@ControllerAdvice
public class GlobalModelAdvice {
    private final UserRepository userRepository;

    public GlobalModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentAccount")
    public Object currentAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) return null;
        var account = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (account != null) {
            LocaleContextHolder.setLocale(Locale.forLanguageTag("en".equals(account.getLanguage()) ? "en" : "pt-BR"));
        }
        return account;
    }
}
