package com.teamportal.user.dto;

import com.teamportal.user.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos editables de un usuario. La contraseña se cambia con PATCH /api/users/{id}/password. */
public class UserUpdateRequest {
    @Schema(description = "Nombre completo", example = "Ana Gómez")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String name;

    @Schema(description = "Email único; se guarda en minúsculas", example = "ana@empresa.com")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;

    @Schema(description = "Rol del usuario", example = "LEADER")
    @NotNull(message = "El rol es obligatorio")
    private Role role;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
