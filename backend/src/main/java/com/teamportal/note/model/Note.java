package com.teamportal.note.model;

import com.teamportal.board.model.Board;
import com.teamportal.user.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteStatus status;

    @Column(name = "position_x")
    private Integer positionX = 0;

    @Column(name = "position_y")
    private Integer positionY = 0;

    /** Tablero al que pertenece la nota. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /** Usuario que creó la nota (dueño). Puede ser null en notas anteriores a los roles. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Momento en que la nota pasó a COMPLETED (null si no está completada). */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Cambia el estado y mantiene al día la fecha de completado. */
    public void changeStatus(NoteStatus newStatus) {
        if (newStatus == NoteStatus.COMPLETED && this.status != NoteStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        } else if (newStatus != NoteStatus.COMPLETED) {
            this.completedAt = null;
        }
        this.status = newStatus;
    }

    /** Indica si la nota fue creada por el usuario con el id indicado. */
    public boolean isOwnedBy(Long userId) {
        return createdBy != null && userId != null && userId.equals(createdBy.getId());
    }

    // Getters and Setters
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

    public Board getBoard() { return board; }
    public void setBoard(Board board) { this.board = board; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
