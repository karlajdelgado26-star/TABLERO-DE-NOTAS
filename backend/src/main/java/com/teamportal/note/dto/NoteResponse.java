package com.teamportal.note.dto;

import com.teamportal.note.model.Note;
import com.teamportal.note.model.NoteStatus;
import com.teamportal.user.dto.UserSummary;
import java.time.LocalDateTime;

public class NoteResponse {
    private Long id;
    private Long boardId;
    private String title;
    private String content;
    private NoteStatus status;
    private Integer positionX;
    private Integer positionY;
    private UserSummary createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    /** true si el usuario actual puede editar y mover la nota. */
    private boolean canEdit;
    /** true si el usuario actual puede eliminar la nota. */
    private boolean canDelete;

    public NoteResponse(Note note, boolean canEdit, boolean canDelete) {
        this.id = note.getId();
        this.boardId = note.getBoard().getId();
        this.title = note.getTitle();
        this.content = note.getContent();
        this.status = note.getStatus();
        this.positionX = note.getPositionX();
        this.positionY = note.getPositionY();
        this.createdBy = UserSummary.from(note.getCreatedBy());
        this.createdAt = note.getCreatedAt();
        this.updatedAt = note.getUpdatedAt();
        this.completedAt = note.getCompletedAt();
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public NoteStatus getStatus() { return status; }
    public void setStatus(NoteStatus status) { this.status = status; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public UserSummary getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserSummary createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
