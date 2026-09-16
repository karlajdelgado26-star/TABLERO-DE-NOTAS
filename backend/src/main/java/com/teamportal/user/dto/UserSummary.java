package com.teamportal.user.dto;

import com.teamportal.user.model.User;

/**
 * Datos mínimos de un usuario para mostrar como autor de un tablero o una nota.
 * Si el usuario fue desactivado se sigue mostrando su nombre con active = false.
 */
public record UserSummary(Long id, String name, boolean active) {

    public static UserSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummary(user.getId(), user.getName(), user.isActive());
    }
}
