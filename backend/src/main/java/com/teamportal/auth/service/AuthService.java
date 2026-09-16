package com.teamportal.auth.service;

import com.teamportal.auth.dto.LoginRequest;
import com.teamportal.auth.dto.LoginResponse;
import com.teamportal.exception.ResourceNotFoundException;
import com.teamportal.security.JwtUtil;
import com.teamportal.user.dto.UserResponse;
import com.teamportal.user.model.User;
import com.teamportal.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    public LoginResponse authenticate(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase(Locale.ROOT);

        // Lanza AuthenticationException (401) si la contraseña es incorrecta o el usuario está inactivo
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(user.getEmail());

        return new LoginResponse(token, new UserResponse(user));
    }

    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::new)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}
