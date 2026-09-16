package com.teamportal.dashboard.dto;

import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import com.teamportal.note.model.NoteStatus;
import com.teamportal.user.dto.UserSummary;
import com.teamportal.user.model.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Todas las métricas del dashboard calculadas sobre el mismo conjunto filtrado. */
public record DashboardResponse(
        LocalDateTime generatedAt,
        Totals totals,
        List<CountItem> notesByStatus,
        List<CountItem> boardsByType,
        List<CountItem> boardsByStatus,
        List<BoardProgress> boards,
        List<UserProgress> users,
        UserHighlight mostNotes,
        UserHighlight fewestNotes,
        List<CountItem> activityByAction,
        Timeline timeline,
        List<NoteRow> notes
) {

    public record Totals(
            long boards,
            long notes,
            long pending,
            long inProgress,
            long completed,
            double completionRate,
            long contributors,
            long activities
    ) {
    }

    /** Conteo genérico: key es el valor del enum (PENDING, PROJECT, ACTIVE, NOTE_CREATED...). */
    public record CountItem(String key, long count) {
    }

    public record StatusCounts(long pending, long inProgress, long completed, long total, double completionRate) {
    }

    public record BoardProgress(
            Long id,
            String name,
            BoardType type,
            BoardStatus status,
            StatusCounts notes,
            LocalDateTime lastActivityAt
    ) {
    }

    public record UserProgress(
            Long id,
            String name,
            Role role,
            boolean active,
            StatusCounts notes,
            long activities,
            LocalDateTime lastActivityAt
    ) {
    }

    public record UserHighlight(Long id, String name, long notes) {
    }

    /** granularity: DAY o WEEK (cada punto es el lunes de la semana). */
    public record Timeline(LocalDate from, LocalDate to, String granularity, List<TimelinePoint> points) {
    }

    public record TimelinePoint(LocalDate date, long created, long completed) {
    }

    public record NoteRow(
            Long id,
            String title,
            NoteStatus status,
            Long boardId,
            String boardName,
            BoardType boardType,
            BoardStatus boardStatus,
            UserSummary createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime completedAt
    ) {
    }
}
