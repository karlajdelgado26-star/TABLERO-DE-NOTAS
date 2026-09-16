package com.teamportal.user.model;

/**
 * Roles del sistema. Cada rol incluye los permisos del rol inferior:
 * <ul>
 *   <li>ADMIN (Administrador): gestiona usuarios y además tiene todo lo del LEADER.</li>
 *   <li>LEADER (Líder): crea, modifica y elimina tableros, puede editar o eliminar
 *       cualquier nota y además tiene todo lo del USER.</li>
 *   <li>USER (Usuario): ve los tableros, crea notas y solo edita, mueve o elimina las suyas.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    LEADER,
    USER;

    /** Crear, modificar, cambiar contraseña y desactivar usuarios. */
    public boolean canManageUsers() {
        return this == ADMIN;
    }

    /** Crear, modificar y eliminar tableros. */
    public boolean canManageBoards() {
        return this == ADMIN || this == LEADER;
    }

    /** Editar, mover y eliminar notas creadas por otros usuarios. */
    public boolean canModerateNotes() {
        return this == ADMIN || this == LEADER;
    }
}
