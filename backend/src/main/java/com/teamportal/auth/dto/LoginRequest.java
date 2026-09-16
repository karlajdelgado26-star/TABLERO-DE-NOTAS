package com.teamportal.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @Schema(description = "Email del usuario", example = "admin@demo.com")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @Schema(description = "Contraseña", example = "TuContraseña")
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
