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
            createDemoUser(userRepository, passwordEncoder, "Admin Demo", "admin@demo.com", "Admin123*", Role.ADMIN);
            createDemoUser(userRepository, passwordEncoder, "Lider Demo", "lider@demo.com", "Lider123*", Role.LEADER);
            createDemoUser(userRepository, passwordEncoder, "User Demo", "usuario@demo.com", "Usuario123*", Role.USER);
        };
    }

    private void createDemoUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                String name, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
        System.out.println("Demo user created: " + email + " (" + role + ")");
    }
}
