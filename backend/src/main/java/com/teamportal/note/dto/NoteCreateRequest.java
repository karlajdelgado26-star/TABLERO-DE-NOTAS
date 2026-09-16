package com.teamportal.note.dto;

import com.teamportal.note.model.NoteStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NoteCreateRequest {
    @NotBlank(message = "El título es obligatorio")
    private String title;

    private String content;

    @NotNull(message = "El estado es obligatorio")
    private NoteStatus status;

    private Integer positionX = 0;
    private Integer positionY = 0;

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
}
