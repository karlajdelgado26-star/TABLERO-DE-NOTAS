package com.teamportal.dashboard.controller;

import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import com.teamportal.dashboard.dto.DashboardFilter;
import com.teamportal.dashboard.dto.DashboardResponse;
import com.teamportal.dashboard.service.DashboardService;
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
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) BoardType boardType,
            @RequestParam(required = false) BoardStatus boardStatus,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DashboardFilter filter = new DashboardFilter(boardId, boardType, boardStatus, userId, from, to);
        return ResponseEntity.ok(dashboardService.getDashboard(filter));
    }
}
