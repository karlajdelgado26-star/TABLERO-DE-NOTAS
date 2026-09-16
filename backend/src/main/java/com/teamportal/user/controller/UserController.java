package com.teamportal.user.controller;

import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.dto.PasswordChangeRequest;
import com.teamportal.user.dto.UserCreateRequest;
import com.teamportal.user.dto.UserResponse;
import com.teamportal.user.dto.UserUpdateRequest;
import com.teamportal.user.service.UserService;
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
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserUpdateRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    /** Activa (active=true) o desactiva (active=false) un usuario. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> setStatus(@PathVariable Long id,
                                                  @RequestParam boolean active,
                                                  @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(userService.setActiveStatus(id, active, currentUser));
    }

    /** "Eliminar" usuario = desactivarlo. No se borra de la base de datos y sus notas se conservan. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthenticatedUser currentUser) {
        userService.deactivateUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
