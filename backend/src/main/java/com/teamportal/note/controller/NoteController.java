package com.teamportal.note.controller;

import com.teamportal.note.dto.NoteCreateRequest;
import com.teamportal.note.dto.NotePositionRequest;
import com.teamportal.note.dto.NoteResponse;
import com.teamportal.note.dto.NoteUpdateRequest;
import com.teamportal.note.service.NoteService;
import com.teamportal.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notas. Todos los roles autenticados pueden usar estos endpoints;
 * los permisos sobre notas ajenas se validan en {@link NoteService}.
 */
@RestController
@RequestMapping("/api")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/boards/{boardId}/notes")
    public ResponseEntity<List<NoteResponse>> getNotesByBoard(@PathVariable Long boardId,
                                                              @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(noteService.getNotesByBoard(boardId, currentUser));
    }

    @PostMapping("/boards/{boardId}/notes")
    public ResponseEntity<NoteResponse> createNote(@PathVariable Long boardId,
                                                   @Valid @RequestBody NoteCreateRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        NoteResponse response = noteService.createNote(boardId, request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/notes/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable Long id,
                                                   @Valid @RequestBody NoteUpdateRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(noteService.updateNote(id, request, currentUser));
    }

    @PatchMapping("/notes/{id}/position")
    public ResponseEntity<NoteResponse> updatePosition(@PathVariable Long id,
                                                       @Valid @RequestBody NotePositionRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(noteService.updatePosition(id, request, currentUser));
    }

    @DeleteMapping("/notes/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthenticatedUser currentUser) {
        noteService.deleteNote(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
