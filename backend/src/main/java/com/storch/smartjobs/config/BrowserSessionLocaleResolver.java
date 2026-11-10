package com.storch.smartjobs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

public class BrowserSessionLocaleResolver implements LocaleResolver {
    private static final String SESSION_KEY = BrowserSessionLocaleResolver.class.getName() + ".locale";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Object saved = request.getSession(false) == null ? null : request.getSession(false).getAttribute(SESSION_KEY);
        if (saved instanceof Locale locale) return locale;
        return "en".equalsIgnoreCase(request.getLocale().getLanguage())
                ? Locale.ENGLISH : Locale.forLanguageTag("pt-BR");
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (locale == null) {
            if (request.getSession(false) != null) request.getSession(false).removeAttribute(SESSION_KEY);
            return;
        }
        request.getSession(true).setAttribute(SESSION_KEY,
                "en".equalsIgnoreCase(locale.getLanguage()) ? Locale.ENGLISH : Locale.forLanguageTag("pt-BR"));
    }
}
