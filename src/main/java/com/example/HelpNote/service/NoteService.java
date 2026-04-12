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
import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;
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

    public NoteService(NoteRepository noteRepository, AtaRepository ataRepository, AiService aiService) {
        this.noteRepository = noteRepository;
        this.ataRepository = ataRepository;
        this.aiService = aiService;
    }

    @Transactional
    public Note saveTextNote(String title, String content, Long userId) {
        log.info("Salvando nota de texto: '{}' para userId={}", title, userId);
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setUploadDateTime(LocalDateTime.now());
        note.setUserId(userId);

        try {
            AiSuggestionRequest request = new AiSuggestionRequest();
            request.setTitle(title);
            request.setText(content);
            AiSuggestionResponse suggestion = aiService.generateSuggestion(request);
            if (suggestion != null && suggestion.getKeywords() != null) {
                note.setKeywords(String.join(", ", suggestion.getKeywords()));
            }
        } catch (Exception e) {
            log.warn("Erro ao processar IA para nota '{}': {}", title, e.getMessage());
        }

        return noteRepository.save(note);
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

        Note note = new Note(title, filePath.toString(), LocalDateTime.now());
        note.setUserId(userId);

        // Transcribe audio via Groq Whisper
        try {
            log.info("Transcrevendo áudio: {}", uniqueFileName);
            String transcription = aiService.transcribeAudio(filePath, audioFile.getOriginalFilename());
            note.setTranscription(transcription);

            // Analyze the transcription
            if (transcription != null && !transcription.isBlank()) {
                String analysis = aiService.analyzeTranscript(transcription);
                note.setSummary(analysis);
            }
        } catch (Exception e) {
            log.warn("Erro ao transcrever áudio '{}': {}", uniqueFileName, e.getMessage());
        }

        return noteRepository.save(note);
    }

    @Transactional
    public Ata saveMeetingNote(String title, String transcription, Long userId) {
        log.info("Salvando ata de reunião: '{}' para userId={}", title, userId);
        Ata ata = new Ata();
        ata.setTitle(title);
        ata.setTranscription(transcription);
        ata.setUploadDateTime(LocalDateTime.now());
        ata.setUserId(userId);

        try {
            String summary = aiService.generateMeetingMinutes(transcription, title);
            ata.setSummary(summary);
        } catch (Exception e) {
            log.warn("Erro ao gerar resumo da ata '{}': {}", title, e.getMessage());
            ata.setSummary("Resumo não disponível no momento.");
        }

        return ataRepository.save(ata);
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

    @Transactional
    public Optional<Note> updateNote(Long id, String title, String content, Long userId) {
        return noteRepository.findByIdVisible(id, userId).map(note -> {
            note.setTitle(title);
            note.setContent(content);
            try {
                AiSuggestionRequest request = new AiSuggestionRequest();
                request.setTitle(title);
                request.setText(content);
                AiSuggestionResponse suggestion = aiService.generateSuggestion(request);
                if (suggestion != null && suggestion.getKeywords() != null) {
                    note.setKeywords(String.join(", ", suggestion.getKeywords()));
                }
            } catch (Exception e) {
                log.warn("Erro ao regenerar keywords para nota id={}: {}", id, e.getMessage());
            }
            return noteRepository.save(note);
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

    public Page<Note> getNotesPaged(Long userId, int page, int size) {
        return noteRepository.findVisibleByUserPaged(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDateTime")));
    }
}
