package com.teamportal.auth.controller;

import com.teamportal.auth.dto.LoginRequest;
import com.teamportal.auth.dto.LoginResponse;
import com.teamportal.auth.service.AuthService;
import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    /** Datos actualizados del usuario autenticado (por ejemplo, si el administrador le cambió el rol). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(authService.getCurrentUser(currentUser.getId()));
    }
}
