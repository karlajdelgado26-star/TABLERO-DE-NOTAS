package com.teamportal.board.repository;

import com.teamportal.board.model.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @EntityGraph(attributePaths = "createdBy")
    List<Board> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "createdBy")
    Optional<Board> findWithCreatorById(Long id);
}
