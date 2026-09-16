package com.teamportal.board.controller;

import com.teamportal.board.dto.BoardRequest;
import com.teamportal.board.dto.BoardResponse;
import com.teamportal.board.service.BoardService;
import com.teamportal.exception.ErrorResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@Tag(name = "2. Tableros", description = "Consulta (todos los roles) y administración (LEADER y ADMIN) de tableros")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** Todos los roles pueden ver los tableros. */
    @GetMapping
    @Operation(summary = "Listar tableros",
            description = "Todos los tableros ordenados por nombre, con su tipo, estado, autor y cantidad de notas. Roles: todos.")
    public ResponseEntity<List<BoardResponse>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un tablero", description = "Roles: todos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tablero encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Tablero no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BoardResponse> getBoard(@Parameter(description = "Id del tablero") @PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoard(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LEADER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear tablero",
            description = "Si no se envían, el tipo es PROJECT y el estado ACTIVE. Queda registrado como BOARD_CREATED. Roles: LEADER, ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tablero creado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Nombre vacío o demasiado largo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El rol USER no puede crear tableros",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BoardResponse> createBoard(@Valid @RequestBody BoardRequest request,
                                                     @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new ResponseEntity<>(boardService.createBoard(request, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LEADER')")
    @Operation(summary = "Modificar tablero",
            description = "Cambia nombre, descripción, tipo o estado. Si el tipo o el estado no se envían se conservan. "
                    + "Registra BOARD_UPDATED y/o BOARD_STATUS_CHANGED. Roles: LEADER, ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tablero modificado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "El rol USER no puede modificar tableros",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tablero no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BoardResponse> updateBoard(@Parameter(description = "Id del tablero") @PathVariable Long id,
                                                     @Valid @RequestBody BoardRequest request,
                                                     @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(boardService.updateBoard(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LEADER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar tablero",
            description = "Elimina el tablero y todas sus notas. Registra BOARD_DELETED. Roles: LEADER, ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tablero eliminado", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol USER no puede eliminar tableros",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tablero no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteBoard(@Parameter(description = "Id del tablero") @PathVariable Long id,
                                            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        boardService.deleteBoard(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
