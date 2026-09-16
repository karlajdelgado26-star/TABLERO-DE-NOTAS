package com.teamportal.note.dto;

import jakarta.validation.constraints.NotNull;

public class NotePositionRequest {
    @NotNull(message = "La posición X es obligatoria")
    private Integer positionX;

    @NotNull(message = "La posición Y es obligatoria")
    private Integer positionY;

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }
}
