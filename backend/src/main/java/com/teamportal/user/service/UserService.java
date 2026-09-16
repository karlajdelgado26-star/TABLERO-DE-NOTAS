package com.teamportal.user.service;

import com.teamportal.exception.BusinessRuleException;
import com.teamportal.exception.ResourceNotFoundException;
import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.dto.PasswordChangeRequest;
import com.teamportal.user.dto.UserCreateRequest;
import com.teamportal.user.dto.UserResponse;
import com.teamportal.user.dto.UserUpdateRequest;
import com.teamportal.user.model.Role;
import com.teamportal.user.model.User;
import com.teamportal.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Administración de usuarios (solo ADMIN).
 * Los usuarios no se borran de la base de datos: "eliminar" los desactiva (active = false),
 * así sus notas conservan el nombre del autor.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by("name")).stream()
                .map(UserResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return new UserResponse(findUser(id));
    }

    public UserResponse createUser(UserCreateRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("El email ya está en uso");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);

        return new UserResponse(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request, AuthenticatedUser currentUser) {
        User user = findUser(id);
        String email = normalizeEmail(request.getEmail());

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("El email ya está en uso");
        }

        if (request.getRole() != user.getRole()) {
            if (user.getId().equals(currentUser.getId())) {
                throw new BusinessRuleException("No puedes cambiar tu propio rol");
            }
            if (user.getRole() == Role.ADMIN && user.isActive()) {
                validateNotLastActiveAdmin();
            }
        }

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());

        return new UserResponse(userRepository.save(user));
    }

    public void changePassword(Long id, PasswordChangeRequest request) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** "Eliminar" un usuario: se desactiva y pierde el acceso, pero sus notas se conservan. */
    public UserResponse deactivateUser(Long id, AuthenticatedUser currentUser) {
        User user = findUser(id);

        if (user.getId().equals(currentUser.getId())) {
            throw new BusinessRuleException("No puedes desactivar tu propio usuario");
        }
        if (user.getRole() == Role.ADMIN && user.isActive()) {
            validateNotLastActiveAdmin();
        }

        user.setActive(false);
        return new UserResponse(userRepository.save(user));
    }

    public UserResponse activateUser(Long id) {
        User user = findUser(id);
        user.setActive(true);
        return new UserResponse(userRepository.save(user));
    }

    public UserResponse setActiveStatus(Long id, boolean active, AuthenticatedUser currentUser) {
        return active ? activateUser(id) : deactivateUser(id, currentUser);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private void validateNotLastActiveAdmin() {
        long activeAdminsCount = userRepository.countByRoleAndActiveTrue(Role.ADMIN);
        if (activeAdminsCount <= 1) {
            throw new BusinessRuleException("No se puede realizar la acción. Debe haber al menos un administrador activo.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
