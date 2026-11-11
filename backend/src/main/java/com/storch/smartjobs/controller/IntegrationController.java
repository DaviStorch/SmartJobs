package com.storch.smartjobs.controller;

import com.storch.smartjobs.integration.gmail.GmailIntegrationException;
import com.storch.smartjobs.integration.gmail.GmailOAuthService;
import com.storch.smartjobs.integration.gmail.GmailSyncCoordinator;
import com.storch.smartjobs.service.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Controller
@RequestMapping("/integrations")
public class IntegrationController {
    private static final String OAUTH_STATE = "gmail.oauth.state";
    private final CurrentUserService currentUser;
    private final GmailOAuthService gmail;
    private final GmailSyncCoordinator syncCoordinator;
    private final SecureRandom random = new SecureRandom();

    public IntegrationController(CurrentUserService currentUser, GmailOAuthService gmail,
                                  GmailSyncCoordinator syncCoordinator) {
        this.currentUser = currentUser;
        this.gmail = gmail;
        this.syncCoordinator = syncCoordinator;
    }

    @GetMapping
    public String index(Authentication authentication, Model model) {
        var user = currentUser.get(authentication);
        model.addAttribute("gmailConfigured", gmail.configured());
        model.addAttribute("gmailConnection", gmail.connection(user).orElse(null));
        return "integrations/index";
    }

    @PostMapping("/gmail/connect")
    public String connect(HttpSession session) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(OAUTH_STATE, state);
        return "redirect:" + gmail.authorizationUrl(state);
    }

    @GetMapping("/gmail/callback")
    public String callback(@RequestParam(required = false) String state, @RequestParam(required = false) String code,
                           @RequestParam(required = false) String error, HttpSession session,
                           Authentication authentication, RedirectAttributes redirect) {
        String expectedState = (String) session.getAttribute(OAUTH_STATE);
        session.removeAttribute(OAUTH_STATE);
        if (expectedState == null || state == null || !MessageDigest.isEqual(
                expectedState.getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8))) {
            redirect.addFlashAttribute("error", "A validação OAuth expirou ou falhou. Tente conectar novamente.");
            return "redirect:/integrations";
        }
        if (error != null) {
            redirect.addFlashAttribute("error", "A autorização Google foi cancelada ou negada.");
            return "redirect:/integrations";
        }
        if (code == null || code.isBlank()) {
            redirect.addFlashAttribute("error", "O Google não retornou um código de autorização.");
            return "redirect:/integrations";
        }
        try {
            gmail.connect(currentUser.get(authentication), code);
            redirect.addFlashAttribute("success", "Conta Gmail conectada com escopo somente de leitura.");
        } catch (GmailIntegrationException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/integrations";
    }

    @PostMapping("/gmail/sync")
    public String synchronize(Authentication authentication, RedirectAttributes redirect) {
        try {
            boolean queued = syncCoordinator.enqueue(currentUser.get(authentication).getId());
            redirect.addFlashAttribute("success", queued
                    ? "Sincronização iniciada. Você pode continuar usando o SmartJobs enquanto ela é processada."
                    : "A sincronização Gmail já está em andamento.");
        } catch (GmailIntegrationException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/integrations";
    }

    @PostMapping("/gmail/disconnect")
    public String disconnect(Authentication authentication, RedirectAttributes redirect) {
        boolean revoked = gmail.disconnect(currentUser.get(authentication));
        redirect.addFlashAttribute("success", revoked
                ? "Conta Gmail desconectada."
                : "A conexão local foi removida. O Google não confirmou a revogação; remova o acesso também na sua conta Google.");
        return "redirect:/integrations";
    }
}
