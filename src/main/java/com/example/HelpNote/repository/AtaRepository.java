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

    @Query("SELECT a FROM Ata a WHERE a.userId = :userId ORDER BY a.uploadDateTime DESC")
    List<Ata> findVisibleByUser(@Param("userId") Long userId);

    @Query("SELECT a FROM Ata a WHERE a.userId = :userId ORDER BY a.uploadDateTime DESC")
    Page<Ata> findVisibleByUserPaged(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Ata a WHERE a.id = :id AND a.userId = :userId")
    Optional<Ata> findByIdVisible(@Param("id") Long id, @Param("userId") Long userId);
}
