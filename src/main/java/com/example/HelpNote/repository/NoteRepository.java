package com.example.HelpNote.repository;

import com.example.HelpNote.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n WHERE n.userId = :userId ORDER BY n.uploadDateTime DESC")
    List<Note> findVisibleByUser(@Param("userId") Long userId);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId ORDER BY n.uploadDateTime DESC")
    Page<Note> findVisibleByUserPaged(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.id = :id AND n.userId = :userId")
    Optional<Note> findByIdVisible(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND (" +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(n.keywords) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY n.uploadDateTime DESC")
    List<Note> searchByUser(@Param("userId") Long userId, @Param("q") String q);
}
