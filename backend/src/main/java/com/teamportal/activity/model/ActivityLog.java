package com.teamportal.activity.model;

import com.teamportal.user.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Una acción realizada por un usuario sobre un tablero o una nota.
 * El nombre del tablero y el título de la nota se copian para que el
 * historial se pueda leer aunque luego se eliminen.
 */
@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActivityAction action;

    /** Quién realizó la acción. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "board_name", length = 100)
    private String boardName;

    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "note_title")
    private String noteTitle;

    /** Dueño de la nota afectada (puede ser distinto de quien hizo la acción). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_owner_id")
    private User noteOwner;

    /** Valor anterior, por ejemplo el estado antes del cambio. */
    @Column(name = "from_value", length = 30)
    private String fromValue;

    /** Valor nuevo, por ejemplo el estado después del cambio. */
    @Column(name = "to_value", length = 30)
    private String toValue;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ActivityAction getAction() { return action; }
    public void setAction(ActivityAction action) { this.action = action; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }

    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    public String getNoteTitle() { return noteTitle; }
    public void setNoteTitle(String noteTitle) { this.noteTitle = noteTitle; }

    public User getNoteOwner() { return noteOwner; }
    public void setNoteOwner(User noteOwner) { this.noteOwner = noteOwner; }

    public String getFromValue() { return fromValue; }
    public void setFromValue(String fromValue) { this.fromValue = fromValue; }

    public String getToValue() { return toValue; }
    public void setToValue(String toValue) { this.toValue = toValue; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
