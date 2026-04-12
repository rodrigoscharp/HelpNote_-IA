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

    /** Returns notes owned by this user OR old records with no owner (migration compatibility). */
    @Query("SELECT n FROM Note n WHERE n.userId = :userId OR n.userId IS NULL ORDER BY n.uploadDateTime DESC")
    List<Note> findVisibleByUser(@Param("userId") Long userId);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId OR n.userId IS NULL ORDER BY n.uploadDateTime DESC")
    Page<Note> findVisibleByUserPaged(@Param("userId") Long userId, Pageable pageable);

    /** Finds a specific note: owned by user OR old record with no owner. */
    @Query("SELECT n FROM Note n WHERE n.id = :id AND (n.userId = :userId OR n.userId IS NULL)")
    Optional<Note> findByIdVisible(@Param("id") Long id, @Param("userId") Long userId);
}
