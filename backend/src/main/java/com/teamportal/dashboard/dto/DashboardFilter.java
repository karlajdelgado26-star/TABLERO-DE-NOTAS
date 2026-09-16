package com.teamportal.dashboard.dto;

import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;

import java.time.LocalDate;

/**
 * Filtros del dashboard, todos opcionales.
 * {@code from}/{@code to} (inclusivos) filtran las notas por fecha de creación
 * y el historial por fecha de la acción.
 */
public record DashboardFilter(
        Long boardId,
        BoardType boardType,
        BoardStatus boardStatus,
        Long userId,
        LocalDate from,
        LocalDate to
) {
}
