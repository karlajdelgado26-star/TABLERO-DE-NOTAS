package com.teamportal.activity.dto;

import com.teamportal.activity.model.ActivityAction;
import com.teamportal.activity.model.ActivityLog;
import com.teamportal.user.dto.UserSummary;

import java.time.LocalDateTime;

public record ActivityResponse(
        Long id,
        ActivityAction action,
        UserSummary user,
        Long boardId,
        String boardName,
        Long noteId,
        String noteTitle,
        UserSummary noteOwner,
        String fromValue,
        String toValue,
        String details,
        LocalDateTime createdAt
) {
    public static ActivityResponse from(ActivityLog log) {
        return new ActivityResponse(
                log.getId(),
                log.getAction(),
                UserSummary.from(log.getUser()),
                log.getBoardId(),
                log.getBoardName(),
                log.getNoteId(),
                log.getNoteTitle(),
                UserSummary.from(log.getNoteOwner()),
                log.getFromValue(),
                log.getToValue(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
