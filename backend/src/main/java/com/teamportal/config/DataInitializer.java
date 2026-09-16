package com.teamportal.config;

import com.teamportal.user.model.Role;
import com.teamportal.user.model.User;
import com.teamportal.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@demo.com")) {
                User admin = new User();
                admin.setName("Admin Demo");
                admin.setEmail("admin@demo.com");
                admin.setPassword(passwordEncoder.encode("Admin123*"));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                userRepository.save(admin);
                System.out.println("Demo Admin user created.");
            }

            if (!userRepository.existsByEmail("usuario@demo.com")) {
                User user = new User();
                user.setName("User Demo");
                user.setEmail("usuario@demo.com");
                user.setPassword(passwordEncoder.encode("Usuario123*"));
                user.setRole(Role.USER);
                user.setActive(true);
                userRepository.save(user);
                System.out.println("Demo User created.");
            }
        };
    }
}
