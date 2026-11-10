package com.storch.smartjobs.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrowserSessionLocaleResolverTest {
    @Test
    void selectsEnglishFromBrowserUntilAChoiceIsStoredInSession() {
        BrowserSessionLocaleResolver resolver = new BrowserSessionLocaleResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US,en;q=0.9");

        assertEquals(Locale.ENGLISH, resolver.resolveLocale(request));

        resolver.setLocale(request, new MockHttpServletResponse(), Locale.forLanguageTag("pt-BR"));
        assertEquals(Locale.forLanguageTag("pt-BR"), resolver.resolveLocale(request));
    }

    @Test
    void usesBrazilianPortugueseForOtherBrowserLanguages() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "fr-FR,fr;q=0.9");
        assertEquals(Locale.forLanguageTag("pt-BR"), new BrowserSessionLocaleResolver().resolveLocale(request));
    }
}
