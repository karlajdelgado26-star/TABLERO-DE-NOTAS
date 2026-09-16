package com.teamportal.board.dto;

import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de un tablero. Si no se envían, al crear se usa tipo PROJECT y estado ACTIVE;
 * al editar se conservan los valores actuales.
 */
public record BoardRequest(
        @NotBlank(message = "El nombre del tablero es obligatorio")
        @Size(max = 100, message = "El nombre del tablero no puede superar 100 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description,

        BoardType type,

        BoardStatus status
) {
}
