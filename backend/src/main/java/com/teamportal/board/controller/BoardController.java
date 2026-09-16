package com.teamportal.board.controller;

import com.teamportal.board.dto.BoardRequest;
import com.teamportal.board.dto.BoardResponse;
import com.teamportal.board.service.BoardService;
import com.teamportal.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** Todos los roles pueden ver los tableros. */
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoard(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LEADER')")
    public ResponseEntity<BoardResponse> createBoard(@Valid @RequestBody BoardRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new ResponseEntity<>(boardService.createBoard(request, currentUser), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LEADER')")
    public ResponseEntity<BoardResponse> updateBoard(@PathVariable Long id, @Valid @RequestBody BoardRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(boardService.updateBoard(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'LEADER')")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        boardService.deleteBoard(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
