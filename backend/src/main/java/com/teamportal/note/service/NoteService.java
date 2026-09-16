package com.teamportal.note.service;

import com.teamportal.note.dto.NoteCreateRequest;
import com.teamportal.note.dto.NotePositionRequest;
import com.teamportal.note.dto.NoteResponse;
import com.teamportal.note.dto.NoteUpdateRequest;
import com.teamportal.note.model.Note;
import com.teamportal.note.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<NoteResponse> getAllNotes() {
        return noteRepository.findAll().stream()
                .map(NoteResponse::new)
                .collect(Collectors.toList());
    }

    public NoteResponse createNote(NoteCreateRequest request) {
        Note note = new Note();
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setStatus(request.getStatus());
        note.setPositionX(request.getPositionX() != null ? request.getPositionX() : 0);
        note.setPositionY(request.getPositionY() != null ? request.getPositionY() : 0);

        Note savedNote = noteRepository.save(note);
        return new NoteResponse(savedNote);
    }

    public NoteResponse updateNote(Long id, NoteUpdateRequest request) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota no encontrada"));

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setStatus(request.getStatus());

        Note updatedNote = noteRepository.save(note);
        return new NoteResponse(updatedNote);
    }

    public NoteResponse updatePosition(Long id, NotePositionRequest request) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota no encontrada"));

        note.setPositionX(request.getPositionX());
        note.setPositionY(request.getPositionY());

        Note updatedNote = noteRepository.save(note);
        return new NoteResponse(updatedNote);
    }

    public void deleteNote(Long id) {
        if (!noteRepository.existsById(id)) {
            throw new RuntimeException("Nota no encontrada");
        }
        noteRepository.deleteById(id);
    }
}
