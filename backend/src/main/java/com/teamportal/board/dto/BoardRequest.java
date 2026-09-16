package com.teamportal.board.dto;

import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de un tablero. Si no se envían, al crear se usa tipo PROJECT y estado ACTIVE;
 * al editar se conservan los valores actuales.
 */
public record BoardRequest(
        @Schema(description = "Nombre del tablero (máx. 100)", example = "Sprint 12")
        @NotBlank(message = "El nombre del tablero es obligatorio")
        @Size(max = 100, message = "El nombre del tablero no puede superar 100 caracteres")
        String name,

        @Schema(description = "Descripción opcional (máx. 500)", example = "Tareas del sprint de septiembre")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description,

        @Schema(description = "Tipo. Por defecto PROJECT al crear", example = "SPRINT")
        BoardType type,

        @Schema(description = "Estado. Por defecto ACTIVE al crear", example = "ACTIVE")
        BoardStatus status
) {
}
