package com.teamportal.note.dto;

import com.teamportal.note.model.NoteStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NoteUpdateRequest {
    @NotBlank(message = "El título es obligatorio")
    private String title;

    private String content;

    @NotNull(message = "El estado es obligatorio")
    private NoteStatus status;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public NoteStatus getStatus() { return status; }
    public void setStatus(NoteStatus status) { this.status = status; }
}
