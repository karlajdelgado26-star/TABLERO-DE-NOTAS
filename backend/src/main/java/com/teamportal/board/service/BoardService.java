package com.teamportal.board.service;

import com.teamportal.activity.service.ActivityService;
import com.teamportal.board.dto.BoardRequest;
import com.teamportal.board.dto.BoardResponse;
import com.teamportal.board.model.Board;
import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import com.teamportal.board.repository.BoardRepository;
import com.teamportal.exception.ResourceNotFoundException;
import com.teamportal.note.repository.NoteRepository;
import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Tableros: todos los roles pueden verlos; crear, modificar y eliminar
 * está limitado a LEADER y ADMIN desde {@code BoardController}.
 * Cada cambio queda registrado en el historial de actividad.
 */
@Service
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    public BoardService(BoardRepository boardRepository, NoteRepository noteRepository,
                        UserRepository userRepository, ActivityService activityService) {
        this.boardRepository = boardRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> getAllBoards() {
        Map<Long, Long> noteCounts = noteRepository.countNotesGroupedByBoard().stream()
                .collect(Collectors.toMap(NoteRepository.BoardNoteCount::getBoardId, NoteRepository.BoardNoteCount::getTotal));

        return boardRepository.findAllByOrderByNameAsc().stream()
                .map(board -> BoardResponse.from(board, noteCounts.getOrDefault(board.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long id) {
        Board board = boardRepository.findWithCreatorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tablero no encontrado"));
        return BoardResponse.from(board, noteRepository.countByBoardId(id));
    }

    public BoardResponse createBoard(BoardRequest request, AuthenticatedUser currentUser) {
        Board board = new Board();
        board.setName(request.name().trim());
        board.setDescription(normalizeDescription(request.description()));
        board.setType(request.type() != null ? request.type() : BoardType.PROJECT);
        board.setStatus(request.status() != null ? request.status() : BoardStatus.ACTIVE);
        board.setCreatedBy(userRepository.getReferenceById(currentUser.getId()));

        Board saved = boardRepository.save(board);
        activityService.boardCreated(saved, currentUser);
        return BoardResponse.from(saved, 0L);
    }

    public BoardResponse updateBoard(Long id, BoardRequest request, AuthenticatedUser currentUser) {
        Board board = boardRepository.findWithCreatorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tablero no encontrado"));

        String newName = request.name().trim();
        String newDescription = normalizeDescription(request.description());
        BoardType newType = request.type() != null ? request.type() : board.getType();
        BoardStatus newStatus = request.status() != null ? request.status() : board.getStatus();

        boolean dataChanged = !newName.equals(board.getName())
                || !Objects.equals(newDescription, board.getDescription())
                || newType != board.getType();
        BoardStatus previousStatus = board.getStatus();

        board.setName(newName);
        board.setDescription(newDescription);
        board.setType(newType);
        board.setStatus(newStatus);

        Board saved = boardRepository.saveAndFlush(board);
        if (dataChanged) {
            activityService.boardUpdated(saved, currentUser);
        }
        if (previousStatus != newStatus) {
            activityService.boardStatusChanged(saved, previousStatus, currentUser);
        }
        return BoardResponse.from(saved, noteRepository.countByBoardId(id));
    }

    /** Elimina el tablero y todas sus notas. */
    public void deleteBoard(Long id, AuthenticatedUser currentUser) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tablero no encontrado"));
        long deletedNotes = noteRepository.countByBoardId(id);
        activityService.boardDeleted(board, deletedNotes, currentUser);
        noteRepository.deleteAllByBoardId(id);
        boardRepository.delete(board);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
