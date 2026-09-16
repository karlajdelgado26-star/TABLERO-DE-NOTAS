package com.teamportal.user.controller;

import com.teamportal.exception.ErrorResponse;
import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.dto.PasswordChangeRequest;
import com.teamportal.user.dto.UserCreateRequest;
import com.teamportal.user.dto.UserResponse;
import com.teamportal.user.dto.UserUpdateRequest;
import com.teamportal.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administración de usuarios: solo el rol ADMIN. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "4. Usuarios", description = "Administración de usuarios. Todos los endpoints requieren el rol ADMIN (los demás roles reciben 403)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Activos e inactivos, ordenados por nombre.")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getUser(@Parameter(description = "Id del usuario") @PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear usuario",
            description = "El email se guarda en minúsculas y debe ser único. La contraseña se guarda cifrada con BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (email, contraseña de menos de 8 caracteres, rol)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El email ya está en uso",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar usuario",
            description = "Cambia nombre, email y rol. Un administrador no puede cambiar su propio rol "
                    + "ni quitarle el rol al último administrador activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario modificado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email repetido, cambio del propio rol o último administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateUser(@Parameter(description = "Id del usuario") @PathVariable Long id,
                                                   @Valid @RequestBody UserUpdateRequest request,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser));
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar contraseña", description = "Asigna una nueva contraseña (8 a 100 caracteres).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña cambiada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Contraseña demasiado corta o vacía",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> changePassword(@Parameter(description = "Id del usuario") @PathVariable Long id,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    /** Activa (active=true) o desactiva (active=false) un usuario. */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Activar o desactivar usuario",
            description = "active=false equivale a eliminar; active=true lo reactiva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "409", description = "Desactivarse a sí mismo o al último administrador activo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> setStatus(@Parameter(description = "Id del usuario") @PathVariable Long id,
                                                  @Parameter(description = "true = activo, false = inactivo") @RequestParam boolean active,
                                                  @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(userService.setActiveStatus(id, active, currentUser));
    }

    /** "Eliminar" usuario = desactivarlo. No se borra de la base de datos y sus notas se conservan. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar (desactivar) usuario",
            description = "No borra el registro: pone active=false. El usuario ya no puede iniciar sesión, su token deja "
                    + "de funcionar y sus notas se conservan con su nombre.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario desactivado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Desactivarse a sí mismo o al último administrador activo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUser(@Parameter(description = "Id del usuario") @PathVariable Long id,
                                           @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        userService.deactivateUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
