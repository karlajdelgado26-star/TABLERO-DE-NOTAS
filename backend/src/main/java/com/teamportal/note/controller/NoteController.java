package com.teamportal.note.controller;

import com.teamportal.exception.ErrorResponse;
import com.teamportal.note.dto.NoteCreateRequest;
import com.teamportal.note.dto.NotePositionRequest;
import com.teamportal.note.dto.NoteResponse;
import com.teamportal.note.dto.NoteUpdateRequest;
import com.teamportal.note.service.NoteService;
import com.teamportal.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "3. Notas", description = "Notas de un tablero. USER solo modifica sus propias notas; LEADER y ADMIN, cualquiera")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/boards/{boardId}/notes")
    @Operation(summary = "Listar notas de un tablero",
            description = "Cada nota incluye canEdit y canDelete calculados para el usuario del token. Roles: todos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notas del tablero", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Tablero no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<NoteResponse>> getNotesByBoard(@Parameter(description = "Id del tablero") @PathVariable Long boardId,
                                                              @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(noteService.getNotesByBoard(boardId, currentUser));
    }

    @PostMapping("/boards/{boardId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear nota",
            description = "El usuario del token queda como autor. Registra NOTE_CREATED. Roles: todos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nota creada", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Falta el título o el estado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tablero no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NoteResponse> createNote(@Parameter(description = "Id del tablero") @PathVariable Long boardId,
                                                   @Valid @RequestBody NoteCreateRequest request,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        NoteResponse response = noteService.createNote(boardId, request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/notes/{id}")
    @Operation(summary = "Editar nota",
            description = "Cambia título, contenido y estado. Registra NOTE_UPDATED y/o NOTE_STATUS_CHANGED. "
                    + "Roles: autor de la nota, LEADER, ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota editada", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Un USER intentó editar una nota ajena",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nota no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NoteResponse> updateNote(@Parameter(description = "Id de la nota") @PathVariable Long id,
                                                   @Valid @RequestBody NoteUpdateRequest request,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(noteService.updateNote(id, request, currentUser));
    }

    @PatchMapping("/notes/{id}/position")
    @Operation(summary = "Mover nota",
            description = "Guarda la posición (x, y) en el lienzo; los valores negativos se ajustan a 0. Registra NOTE_MOVED. "
                    + "Roles: autor de la nota, LEADER, ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posición guardada", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Un USER intentó mover una nota ajena",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nota no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NoteResponse> updatePosition(@Parameter(description = "Id de la nota") @PathVariable Long id,
                                                       @Valid @RequestBody NotePositionRequest request,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(noteService.updatePosition(id, request, currentUser));
    }

    @DeleteMapping("/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar nota",
            description = "Registra NOTE_DELETED. Roles: autor de la nota, LEADER, ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Nota eliminada", content = @Content),
            @ApiResponse(responseCode = "403", description = "Un USER intentó eliminar una nota ajena",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nota no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteNote(@Parameter(description = "Id de la nota") @PathVariable Long id,
                                           @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        noteService.deleteNote(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
