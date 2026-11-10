package com.storch.smartjobs.service;

import com.storch.smartjobs.dto.RegisterForm;
import com.storch.smartjobs.entity.AppUser;
import com.storch.smartjobs.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterForm form) {
        register(form, "pt");
    }

    @Transactional
    public void register(RegisterForm form, String language) {
        String email = form.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail.");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("As senhas não conferem.");
        }
        if (form.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve ter no máximo 72 bytes em UTF-8.");
        }
        AppUser user = new AppUser(form.getName().trim(), email, passwordEncoder.encode(form.getPassword()));
        user.setLanguage("en".equals(language) ? "en" : "pt");
        userRepository.save(user);
    }
}
