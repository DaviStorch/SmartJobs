package com.storch.smartjobs.service;

import com.storch.smartjobs.dto.RegisterForm;
import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Test
    void storesAnEncodedPasswordAndNormalizesEmail() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(encoder.encode("a-strong-password")).thenReturn("$2a$encoded");
        UserService service = new UserService(users, encoder);
        RegisterForm form = new RegisterForm();
        form.setName(" Person ");
        form.setEmail("PERSON@example.com");
        form.setPassword("a-strong-password");
        form.setConfirmPassword("a-strong-password");

        service.register(form);

        verify(users).save(argThat(user -> user.getName().equals("Person")
                && user.getEmail().equals("person@example.com")
                && user.getPasswordHash().equals("$2a$encoded")));
    }

    @Test
    void rejectsAnEmailAlreadyRegistered() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserService service = new UserService(users, encoder);
        RegisterForm form = new RegisterForm();
        form.setEmail("person@example.com");
        form.setPassword("a-strong-password");
        form.setConfirmPassword("different-password");
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.register(form));
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void rejectsMismatchedPasswords() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        UserService service = new UserService(users, encoder);
        RegisterForm form = new RegisterForm();
        form.setEmail("person@example.com");
        form.setPassword("a-strong-password");
        form.setConfirmPassword("different-password");

        assertThrows(IllegalArgumentException.class, () -> service.register(form));
        verify(users, never()).save(any(AppUser.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void initializesLanguageFromBrowserLocaleAndKeepsManualPreferenceEditable() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(encoder.encode("a-strong-password")).thenReturn("encoded");
        RegisterForm form = new RegisterForm();
        form.setName("Person");
        form.setEmail("person@example.com");
        form.setPassword("a-strong-password");
        form.setConfirmPassword("a-strong-password");

        new UserService(users, encoder).register(form, "en");

        verify(users).save(argThat(user -> user.getLanguage().equals("en")));
    }
}
