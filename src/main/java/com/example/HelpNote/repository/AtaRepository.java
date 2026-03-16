package com.example.HelpNote.repository;

import com.example.HelpNote.domain.Ata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtaRepository extends JpaRepository<Ata, Long> {
    List<Ata> findAllByOrderByUploadDateTimeDesc();
}
