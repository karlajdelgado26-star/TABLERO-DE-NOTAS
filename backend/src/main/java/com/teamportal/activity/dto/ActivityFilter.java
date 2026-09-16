package com.teamportal.activity.dto;

import com.teamportal.activity.model.ActivityAction;

import java.time.LocalDate;

/** Filtros del historial. Todos opcionales; las fechas son inclusivas. */
public record ActivityFilter(
        Long userId,
        Long boardId,
        ActivityAction action,
        LocalDate from,
        LocalDate to
) {
}
