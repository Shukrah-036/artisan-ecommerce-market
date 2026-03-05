package com.artisanmarket;

import com.artisanmarket.user.Role;
import com.artisanmarket.user.User;
import com.artisanmarket.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        String adminEmail = "admin@artisanmarket.com";

        // Check first so restarting the app doesn't try to create a duplicate
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists - skipping creation");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("admin123"));  // change this
        admin.setDisplayName("Admin");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.info("Admin account created: {}", adminEmail);
    }
}