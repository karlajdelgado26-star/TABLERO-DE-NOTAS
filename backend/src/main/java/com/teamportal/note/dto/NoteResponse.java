package com.teamportal.note.dto;

import com.teamportal.note.model.Note;
import com.teamportal.note.model.NoteStatus;
import java.time.LocalDateTime;

public class NoteResponse {
    private Long id;
    private String title;
    private String content;
    private NoteStatus status;
    private Integer positionX;
    private Integer positionY;
    private LocalDateTime createdAt;

    public NoteResponse(Note note) {
        this.id = note.getId();
        this.title = note.getTitle();
        this.content = note.getContent();
        this.status = note.getStatus();
        this.positionX = note.getPositionX();
        this.positionY = note.getPositionY();
        this.createdAt = note.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
