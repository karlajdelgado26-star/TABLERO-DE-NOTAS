package com.teamportal.auth.controller;

import com.teamportal.auth.dto.LoginRequest;
import com.teamportal.auth.dto.LoginResponse;
import com.teamportal.auth.service.AuthService;
import com.teamportal.exception.ErrorResponse;
import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Autenticación", description = "Inicio de sesión con JWT y datos del usuario actual")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Valida email y contraseña y devuelve un token JWT (24 h) junto con los datos del usuario. "
                    + "Es el único endpoint de la API que no requiere token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Falta el email o la contraseña",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    /** Datos actualizados del usuario autenticado (por ejemplo, si el administrador le cambió el rol). */
    @GetMapping("/me")
    @Operation(summary = "Usuario actual",
            description = "Devuelve los datos vigentes del usuario dueño del token (nombre, rol, estado).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Sin token, token vencido o usuario desactivado",
                    content = @Content)
    })
    public ResponseEntity<UserResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(authService.getCurrentUser(currentUser.getId()));
    }
}
