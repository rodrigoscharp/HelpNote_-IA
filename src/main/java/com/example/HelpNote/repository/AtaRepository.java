package com.example.HelpNote.repository;

import com.example.HelpNote.domain.Ata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtaRepository extends JpaRepository<Ata, Long> {

    /** Returns atas owned by this user OR old records with no owner (migration compatibility). */
    @Query("SELECT a FROM Ata a WHERE a.userId = :userId OR a.userId IS NULL ORDER BY a.uploadDateTime DESC")
    List<Ata> findVisibleByUser(@Param("userId") Long userId);

    @Query("SELECT a FROM Ata a WHERE a.userId = :userId OR a.userId IS NULL ORDER BY a.uploadDateTime DESC")
    Page<Ata> findVisibleByUserPaged(@Param("userId") Long userId, Pageable pageable);

    /** Finds a specific ata: owned by user OR old record with no owner. */
    @Query("SELECT a FROM Ata a WHERE a.id = :id AND (a.userId = :userId OR a.userId IS NULL)")
    Optional<Ata> findByIdVisible(@Param("id") Long id, @Param("userId") Long userId);
}
