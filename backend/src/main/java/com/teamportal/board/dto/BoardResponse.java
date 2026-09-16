package com.teamportal.board.dto;

import com.teamportal.board.model.Board;
import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import com.teamportal.user.dto.UserSummary;

import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        String name,
        String description,
        BoardType type,
        BoardStatus status,
        UserSummary createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long noteCount
) {
    public static BoardResponse from(Board board, long noteCount) {
        return new BoardResponse(
                board.getId(),
                board.getName(),
                board.getDescription(),
                board.getType(),
                board.getStatus(),
                UserSummary.from(board.getCreatedBy()),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                noteCount
        );
    }
}
