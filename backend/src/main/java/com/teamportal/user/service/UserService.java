package com.teamportal.user.service;

import com.teamportal.user.dto.UserCreateRequest;
import com.teamportal.user.dto.UserResponse;
import com.teamportal.user.dto.UserUpdateRequest;
import com.teamportal.user.model.Role;
import com.teamportal.user.model.User;
import com.teamportal.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está en uso");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);

        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está en uso");
        }

        if (user.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN && user.isActive()) {
            validateLastAdmin();
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return new UserResponse(updatedUser);
    }

    public void toggleActiveStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() == Role.ADMIN && user.isActive() && !active) {
            validateLastAdmin();
        }

        user.setActive(active);
        userRepository.save(user);
    }

    private void validateLastAdmin() {
        long activeAdminsCount = userRepository.countByRoleAndActiveTrue(Role.ADMIN);
        if (activeAdminsCount <= 1) {
            throw new RuntimeException("No se puede realizar la acción. Debe haber al menos un administrador activo.");
        }
    }
}
