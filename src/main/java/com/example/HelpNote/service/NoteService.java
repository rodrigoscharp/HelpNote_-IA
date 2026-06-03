package com.example.HelpNote.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.HelpNote.domain.Ata;
import com.example.HelpNote.domain.Note;
import com.example.HelpNote.repository.AtaRepository;
import com.example.HelpNote.repository.NoteRepository;

@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    @Value("${upload.dir:${user.home}/uploads}")
    private String uploadDir;

    private final NoteRepository noteRepository;
    private final AtaRepository ataRepository;
    private final AiService aiService;
    private final NoteEnrichmentService enrichmentService;

    public NoteService(NoteRepository noteRepository, AtaRepository ataRepository,
                       AiService aiService, NoteEnrichmentService enrichmentService) {
        this.noteRepository = noteRepository;
        this.ataRepository = ataRepository;
        this.aiService = aiService;
        this.enrichmentService = enrichmentService;
    }

    @Transactional
    public Note saveTextNote(String title, String content, Long userId) {
        log.info("Salvando nota de texto: '{}' para userId={}", title, userId);
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setUploadDateTime(LocalDateTime.now());
        note.setUserId(userId);
        Note saved = noteRepository.save(note);
        enrichmentService.enrichTextNote(saved.getId(), title, content);
        return saved;
    }

    @Transactional
    public Note saveAudioFile(MultipartFile audioFile, String title, Long userId) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = audioFile.getOriginalFilename();
        String fileExtension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(audioFile.getInputStream(), filePath);

        Note note = new Note(title, null, LocalDateTime.now());
        note.setUserId(userId);

        try {
            log.info("Transcrevendo áudio: {}", uniqueFileName);
            String transcription = aiService.transcribeAudio(filePath, originalFilename);
            note.setTranscription(transcription);
            Note saved = noteRepository.save(note);

            if (transcription != null && !transcription.isBlank()) {
                enrichmentService.analyzeAudioNote(saved.getId(), transcription);
            }

            tryDeleteFile(filePath);
            return saved;
        } catch (Exception e) {
            log.warn("Erro ao transcrever áudio '{}': {}", uniqueFileName, e.getMessage());
            return noteRepository.save(note);
        }
    }

    @Transactional
    public Ata saveMeetingNote(String title, String transcription, Long userId) {
        log.info("Salvando ata de reunião: '{}' para userId={}", title, userId);
        Ata ata = new Ata();
        ata.setTitle(title);
        ata.setTranscription(transcription);
        ata.setUploadDateTime(LocalDateTime.now());
        ata.setUserId(userId);
        Ata saved = ataRepository.save(ata);
        enrichmentService.generateAtaMinutes(saved.getId(), title, transcription);
        return saved;
    }

    @Transactional
    public Optional<Note> updateNote(Long id, String title, String content, Long userId) {
        return noteRepository.findByIdVisible(id, userId).map(note -> {
            note.setTitle(title);
            note.setContent(content);
            Note saved = noteRepository.save(note);
            enrichmentService.enrichTextNote(saved.getId(), title, content);
            return saved;
        });
    }

    @Transactional
    public boolean deleteNote(Long id, Long userId) {
        Optional<Note> noteOpt = noteRepository.findByIdVisible(id, userId);
        if (noteOpt.isPresent()) {
            noteRepository.delete(noteOpt.get());
            log.info("Nota id={} excluída pelo userId={}", id, userId);
            return true;
        }
        return false;
    }

    public Optional<Note> getNoteById(Long id, Long userId) {
        return noteRepository.findByIdVisible(id, userId);
    }

    public List<Note> getAllNotesByUser(Long userId) {
        return noteRepository.findVisibleByUser(userId);
    }

    public List<Note> searchNotes(Long userId, String query) {
        return noteRepository.searchByUser(userId, query);
    }

    public Page<Note> getNotesPaged(Long userId, int page, int size) {
        return noteRepository.findVisibleByUserPaged(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDateTime")));
    }

    public Optional<Ata> getAtaById(Long id, Long userId) {
        return ataRepository.findByIdVisible(id, userId);
    }

    public List<Ata> getAllAtasByUser(Long userId) {
        return ataRepository.findVisibleByUser(userId);
    }

    public Page<Ata> getAtasPaged(Long userId, int page, int size) {
        return ataRepository.findVisibleByUserPaged(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDateTime")));
    }

    private void tryDeleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Não foi possível excluir arquivo temporário: {}", path);
        }
    }
}
