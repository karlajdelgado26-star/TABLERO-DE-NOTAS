package com.teamportal.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class NotePositionRequest {
    @Schema(description = "Posición horizontal (px)", example = "320")
    @NotNull(message = "La posición X es obligatoria")
    private Integer positionX;

    @Schema(description = "Posición vertical (px)", example = "140")
    @NotNull(message = "La posición Y es obligatoria")
    private Integer positionY;

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }
}
