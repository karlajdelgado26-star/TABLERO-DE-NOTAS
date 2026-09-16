package com.teamportal.note.service;

import com.teamportal.activity.service.ActivityService;
import com.teamportal.board.model.Board;
import com.teamportal.board.repository.BoardRepository;
import com.teamportal.exception.ForbiddenOperationException;
import com.teamportal.exception.ResourceNotFoundException;
import com.teamportal.note.dto.NoteCreateRequest;
import com.teamportal.note.dto.NotePositionRequest;
import com.teamportal.note.dto.NoteResponse;
import com.teamportal.note.dto.NoteUpdateRequest;
import com.teamportal.note.model.Note;
import com.teamportal.note.model.NoteStatus;
import com.teamportal.note.repository.NoteRepository;
import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Reglas de negocio de las notas:
 * - Cualquier rol puede ver las notas de un tablero y crear notas nuevas.
 * - USER solo puede editar, mover y eliminar las notas que él creó.
 * - LEADER y ADMIN pueden editar, mover y eliminar cualquier nota.
 * Cada cambio queda registrado en el historial de actividad.
 */
@Service
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    public NoteService(NoteRepository noteRepository, BoardRepository boardRepository,
                       UserRepository userRepository, ActivityService activityService) {
        this.noteRepository = noteRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByBoard(Long boardId, AuthenticatedUser currentUser) {
        if (!boardRepository.existsById(boardId)) {
            throw new ResourceNotFoundException("Tablero no encontrado");
        }
        return noteRepository.findByBoardIdOrderByIdAsc(boardId).stream()
                .map(note -> toResponse(note, currentUser))
                .toList();
    }

    public NoteResponse createNote(Long boardId, NoteCreateRequest request, AuthenticatedUser currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Tablero no encontrado"));

        Note note = new Note();
        note.setBoard(board);
        note.setCreatedBy(userRepository.getReferenceById(currentUser.getId()));
        note.setTitle(request.getTitle().trim());
        note.setContent(request.getContent());
        note.changeStatus(request.getStatus());
        note.setPositionX(request.getPositionX() != null ? request.getPositionX() : 0);
        note.setPositionY(request.getPositionY() != null ? request.getPositionY() : 0);

        Note saved = noteRepository.save(note);
        activityService.noteCreated(saved, currentUser);
        return toResponse(saved, currentUser);
    }

    public NoteResponse updateNote(Long id, NoteUpdateRequest request, AuthenticatedUser currentUser) {
        Note note = findNote(id);
        if (!canEdit(note, currentUser)) {
            throw new ForbiddenOperationException("Solo puedes editar las notas que tú creaste");
        }

        String newTitle = request.getTitle().trim();
        boolean textChanged = !newTitle.equals(note.getTitle())
                || !Objects.equals(emptyToNull(request.getContent()), emptyToNull(note.getContent()));
        NoteStatus previousStatus = note.getStatus();

        note.setTitle(newTitle);
        note.setContent(request.getContent());
        note.changeStatus(request.getStatus());

        Note saved = noteRepository.saveAndFlush(note);
        if (textChanged) {
            activityService.noteUpdated(saved, currentUser);
        }
        if (previousStatus != saved.getStatus()) {
            activityService.noteStatusChanged(saved, previousStatus, currentUser);
        }
        return toResponse(saved, currentUser);
    }

    public NoteResponse updatePosition(Long id, NotePositionRequest request, AuthenticatedUser currentUser) {
        Note note = findNote(id);
        if (!canEdit(note, currentUser)) {
            throw new ForbiddenOperationException("Solo puedes mover las notas que tú creaste");
        }

        int newX = Math.max(0, request.getPositionX());
        int newY = Math.max(0, request.getPositionY());
        boolean moved = !Objects.equals(note.getPositionX(), newX) || !Objects.equals(note.getPositionY(), newY);

        note.setPositionX(newX);
        note.setPositionY(newY);

        Note saved = noteRepository.saveAndFlush(note);
        if (moved) {
            activityService.noteMoved(saved, currentUser);
        }
        return toResponse(saved, currentUser);
    }

    public void deleteNote(Long id, AuthenticatedUser currentUser) {
        Note note = findNote(id);
        if (!canDelete(note, currentUser)) {
            throw new ForbiddenOperationException("Solo puedes eliminar las notas que tú creaste");
        }
        activityService.noteDeleted(note, currentUser);
        noteRepository.delete(note);
    }

    /** Editar o mover: el dueño de la nota, o un rol que modera notas (LEADER/ADMIN). */
    public boolean canEdit(Note note, AuthenticatedUser user) {
        return user.getRole().canModerateNotes() || note.isOwnedBy(user.getId());
    }

    /** Eliminar: el dueño de la nota, o un rol que modera notas (LEADER/ADMIN). */
    public boolean canDelete(Note note, AuthenticatedUser user) {
        return user.getRole().canModerateNotes() || note.isOwnedBy(user.getId());
    }

    private Note findNote(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota no encontrada"));
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private NoteResponse toResponse(Note note, AuthenticatedUser user) {
        return new NoteResponse(note, canEdit(note, user), canDelete(note, user));
    }
}
