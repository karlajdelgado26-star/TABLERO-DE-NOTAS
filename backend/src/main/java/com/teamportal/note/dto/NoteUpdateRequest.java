package com.teamportal.note.dto;

import com.teamportal.note.model.NoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NoteUpdateRequest {
    @Schema(description = "Título de la nota", example = "Configurar CI")
    @NotBlank(message = "El título es obligatorio")
    private String title;

    @Schema(description = "Texto libre (opcional)", example = "Falta el despliegue automático")
    private String content;

    @Schema(description = "Nuevo estado; al pasar a COMPLETED se guarda completedAt", example = "IN_PROGRESS")
    @NotNull(message = "El estado es obligatorio")
    private NoteStatus status;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public NoteStatus getStatus() { return status; }
    public void setStatus(NoteStatus status) { this.status = status; }
}
