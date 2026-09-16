package com.teamportal.note.controller;

import com.teamportal.note.dto.NoteCreateRequest;
import com.teamportal.note.dto.NotePositionRequest;
import com.teamportal.note.dto.NoteResponse;
import com.teamportal.note.dto.NoteUpdateRequest;
import com.teamportal.note.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAllNotes() {
        return ResponseEntity.ok(noteService.getAllNotes());
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody NoteCreateRequest request) {
        NoteResponse response = noteService.createNote(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable Long id, @Valid @RequestBody NoteUpdateRequest request) {
        return ResponseEntity.ok(noteService.updateNote(id, request));
    }

    @PatchMapping("/{id}/position")
    public ResponseEntity<NoteResponse> updatePosition(@PathVariable Long id, @Valid @RequestBody NotePositionRequest request) {
        return ResponseEntity.ok(noteService.updatePosition(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
