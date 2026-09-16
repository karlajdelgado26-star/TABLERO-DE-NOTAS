package com.teamportal.note.repository;

import com.teamportal.note.model.Note;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    @EntityGraph(attributePaths = "createdBy")
    List<Note> findByBoardIdOrderByIdAsc(Long boardId);

    long countByBoardId(Long boardId);

    /** Todas las notas con su tablero y autor cargados (para el dashboard). */
    @Query("select n from Note n join fetch n.board left join fetch n.createdBy")
    List<Note> findAllWithBoardAndCreator();

    @Query("select n.board.id as boardId, count(n) as total from Note n group by n.board.id")
    List<BoardNoteCount> countNotesGroupedByBoard();

    @Modifying
    @Query("delete from Note n where n.board.id = :boardId")
    int deleteAllByBoardId(@Param("boardId") Long boardId);

    interface BoardNoteCount {
        Long getBoardId();
        Long getTotal();
    }
}
