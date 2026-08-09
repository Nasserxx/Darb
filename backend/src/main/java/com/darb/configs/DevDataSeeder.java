package com.darb.configs;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"dev", "docker"})
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("SUPER_ADMIN", "admin@darb.app", "Admin123!", "Darb Admin");
        seed("MOSQUE_ADMIN", "mosque.admin@darb.app", "MosqueAdmin1!", "Mosque Admin");
        seed("TEACHER", "teacher@darb.app", "Teacher123!", "Darb Teacher");
        seed("STUDENT", "student@darb.app", "Student123!", "Darb Student");
        seed("PARENT", "parent@darb.app", "Parent123!", "Darb Parent");
    }

    private void seed(String roleName, String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.valueOf(roleName))
                .isActive(true)
                .build());
        log.info("Seeded {} user: {}", roleName, email);
    }
}
