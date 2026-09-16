package com.teamportal.activity.controller;

import com.teamportal.activity.dto.ActivityFilter;
import com.teamportal.activity.dto.ActivityResponse;
import com.teamportal.activity.dto.PageResponse;
import com.teamportal.activity.model.ActivityAction;
import com.teamportal.activity.service.ActivityService;
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

/** Historial de actividad. Lo pueden ver todos los roles. */
@RestController
@RequestMapping("/api/activity")
@Tag(name = "6. Actividad", description = "Historial de quién creó, editó, movió, cambió de estado o eliminó notas y tableros. Roles: todos")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @Operation(summary = "Historial de actividad",
            description = "Paginado, de la acción más reciente a la más antigua. Todos los filtros son opcionales.")
    public ResponseEntity<PageResponse<ActivityResponse>> search(
            @Parameter(description = "Solo acciones hechas por este usuario") @RequestParam(required = false) Long userId,
            @Parameter(description = "Solo acciones sobre este tablero") @RequestParam(required = false) Long boardId,
            @Parameter(description = "Solo este tipo de acción") @RequestParam(required = false) ActivityAction action,
            @Parameter(description = "Desde (inclusive), formato AAAA-MM-DD", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta (inclusive), formato AAAA-MM-DD", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Página, empieza en 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Elementos por página (1 a 100)") @RequestParam(defaultValue = "20") int size) {
        ActivityFilter filter = new ActivityFilter(userId, boardId, action, from, to);
        return ResponseEntity.ok(activityService.search(filter, page, size));
    }
}
