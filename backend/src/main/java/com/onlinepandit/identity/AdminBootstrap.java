package com.onlinepandit.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserAccountRepository users, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.length() < 16) {
            throw new IllegalStateException("Set both bootstrap admin values; password must be at least 16 characters");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!users.existsByEmail(normalizedEmail)) {
            users.save(new UserAccount("Platform Administrator", normalizedEmail,
                    passwordEncoder.encode(password), null, null, null, UserAccount.Role.ADMIN));
        }
    }
}
