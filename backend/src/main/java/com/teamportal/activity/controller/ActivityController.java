package com.teamportal.activity.controller;

import com.teamportal.activity.dto.ActivityFilter;
import com.teamportal.activity.dto.ActivityResponse;
import com.teamportal.activity.dto.PageResponse;
import com.teamportal.activity.model.ActivityAction;
import com.teamportal.activity.service.ActivityService;
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
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ActivityResponse>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) ActivityAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ActivityFilter filter = new ActivityFilter(userId, boardId, action, from, to);
        return ResponseEntity.ok(activityService.search(filter, page, size));
    }
}
