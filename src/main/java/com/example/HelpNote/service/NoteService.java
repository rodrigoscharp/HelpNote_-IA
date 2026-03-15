package com.example.HelpNote.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.HelpNote.domain.Note;
import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;
import com.example.HelpNote.repository.NoteRepository;

@Service
public class NoteService {

    @Value("${upload.dir:${user.home}/uploads}")
    private String uploadDir;

    private final NoteRepository noteRepository;
    private final AiService aiService;

    public NoteService(NoteRepository noteRepository, AiService aiService) {
        this.noteRepository = noteRepository;
        this.aiService = aiService;
    }

    public Note saveTextNote(String title, String content) {
        System.out.println("Salvando nota de texto: " + title);
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setUploadDateTime(LocalDateTime.now());

        // Process with AI to get keywords and summary
        try {
            AiSuggestionRequest request = new AiSuggestionRequest();
            request.setTitle(title);
            request.setText(content);
            
            AiSuggestionResponse suggestion = aiService.generateSuggestion(request);
            if (suggestion != null && suggestion.getKeywords() != null) {
                note.setKeywords(String.join(", ", suggestion.getKeywords()));
            }
        } catch (Exception e) {
            // Log error but save note anyway
            System.err.println("Erro ao processar IA para nota: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            Note savedNote = noteRepository.save(note);
            System.out.println("Nota salva com ID: " + savedNote.getId());
            return savedNote;
        } catch (Exception e) {
            System.err.println("ERRO CRÍTICO ao salvar nota no banco: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Note saveAudioFile(MultipartFile audioFile, String title) throws IOException {
        // Create the upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique filename
        String originalFilename = audioFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Save the file to the filesystem
        Files.copy(audioFile.getInputStream(), filePath);

        // Create and save a new Note entity
        Note note = new Note(title, filePath.toString(), LocalDateTime.now());
        return noteRepository.save(note);
    }

    public java.util.Optional<Note> getNoteById(Long id) {
        return noteRepository.findById(id);
    }

    public java.util.List<Note> getAllNotesSorted() {
        return noteRepository.findAllByOrderByUploadDateTimeDesc();
    }
}
