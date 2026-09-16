package com.teamportal.dashboard.controller;

import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import com.teamportal.dashboard.dto.DashboardFilter;
import com.teamportal.dashboard.dto.DashboardResponse;
import com.teamportal.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Métricas del dashboard. Las pueden ver todos los roles. */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "5. Dashboard", description = "Métricas de tableros, notas y empleados. Roles: todos (ven la misma información)")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Métricas del dashboard",
            description = "Devuelve en una sola respuesta los totales, notas por estado, tableros por tipo y estado, avance por "
                    + "tablero y por empleado, quién tiene más y menos notas, acciones por tipo, la línea de tiempo "
                    + "(por día hasta 92 días; por semana si el rango es mayor) y el detalle de notas. "
                    + "Todos los filtros son opcionales y se combinan. Sin fechas, la línea de tiempo muestra los últimos 30 días.")
    public ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(description = "Solo este tablero") @RequestParam(required = false) Long boardId,
            @Parameter(description = "Solo tableros de este tipo") @RequestParam(required = false) BoardType boardType,
            @Parameter(description = "Solo tableros en este estado") @RequestParam(required = false) BoardStatus boardStatus,
            @Parameter(description = "Solo notas creadas por este usuario y acciones hechas por él") @RequestParam(required = false) Long userId,
            @Parameter(description = "Desde (inclusive), formato AAAA-MM-DD", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (inclusive), formato AAAA-MM-DD", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DashboardFilter filter = new DashboardFilter(boardId, boardType, boardStatus, userId, from, to);
        return ResponseEntity.ok(dashboardService.getDashboard(filter));
    }
}
