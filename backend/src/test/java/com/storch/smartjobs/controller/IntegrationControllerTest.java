package com.storch.smartjobs.controller;

import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.integration.gmail.GmailOAuthService;
import com.storch.smartjobs.integration.gmail.GmailSyncCoordinator;
import com.storch.smartjobs.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntegrationControllerTest {
    private static final String STATE_KEY = "gmail.oauth.state";

    @Test
    void connectStoresRandomSessionStateAndRedirectsToGoogle() {
        GmailOAuthService gmail = mock(GmailOAuthService.class);
        when(gmail.authorizationUrl(anyString())).thenAnswer(call -> "https://accounts.google.test?state=" + call.getArgument(0));
        IntegrationController controller = new IntegrationController(
                mock(CurrentUserService.class), gmail, mock(GmailSyncCoordinator.class));
        MockHttpSession session = new MockHttpSession();

        String redirect = controller.connect(session);

        String state = (String) session.getAttribute(STATE_KEY);
        assertNotNull(state);
        assertEquals(43, state.length());
        assertEquals("redirect:https://accounts.google.test?state=" + state, redirect);
        verify(gmail).authorizationUrl(state);
    }

    @Test
    void callbackRejectsInvalidStateAndConsumesTheStoredState() {
        GmailOAuthService gmail = mock(GmailOAuthService.class);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_KEY, "expected");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        IntegrationController controller = new IntegrationController(
                mock(CurrentUserService.class), gmail, mock(GmailSyncCoordinator.class));

        String result = controller.callback("attacker-state", "authorization-code", null, session,
                null, redirect);

        assertEquals("redirect:/integrations", result);
        assertNull(session.getAttribute(STATE_KEY));
        assertTrue(redirect.getFlashAttributes().containsKey("error"));
        verifyNoInteractions(gmail);
    }

    @Test
    void callbackAssociatesSuccessfulAuthorizationWithAuthenticatedUser() {
        CurrentUserService currentUser = mock(CurrentUserService.class);
        GmailOAuthService gmail = mock(GmailOAuthService.class);
        AppUser user = mock(AppUser.class);
        var authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(currentUser.get(authentication)).thenReturn(user);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_KEY, "valid-state");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        IntegrationController controller = new IntegrationController(
                currentUser, gmail, mock(GmailSyncCoordinator.class));

        String result = controller.callback("valid-state", "authorization-code", null, session,
                authentication, redirect);

        assertEquals("redirect:/integrations", result);
        assertTrue(redirect.getFlashAttributes().containsKey("success"));
        verify(gmail).connect(user, "authorization-code");
    }

    @Test
    void callbackReportsGoogleAuthorizationCancellation() {
        GmailOAuthService gmail = mock(GmailOAuthService.class);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(STATE_KEY, "valid-state");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        IntegrationController controller = new IntegrationController(
                mock(CurrentUserService.class), gmail, mock(GmailSyncCoordinator.class));

        controller.callback("valid-state", null, "access_denied", session, null, redirect);

        assertTrue(redirect.getFlashAttributes().containsKey("error"));
        verifyNoInteractions(gmail);
    }
}
