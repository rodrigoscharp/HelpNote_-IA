package com.example.HelpNote.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.HelpNote.domain.Note;
import com.example.HelpNote.service.NoteService;
import com.example.HelpNote.service.UsageLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final UsageLimitService usageLimitService;

    public NoteController(NoteService noteService, UsageLimitService usageLimitService) {
        this.noteService = noteService;
        this.usageLimitService = usageLimitService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("title") String title) {
        try {
            Note savedNote = noteService.saveAudioFile(audioFile, title);
            return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
        } catch (IOException e) {
            System.err.println("Erro no upload: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> createNote(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            HttpServletRequest request) {

        Long userId = getUserIdFromSession(request);
        if (userId != null && !usageLimitService.canCreateNote(userId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Limite diário atingido! No plano gratuito você pode criar 1 anotação inteligente por dia.");
            error.put("upgradeRequired", true);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        Note savedNote = noteService.saveTextNote(title, content);

        // Increment usage counter
        if (userId != null) {
            usageLimitService.incrementNoteUsage(userId);
        }

        return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Note>> getAllNotes() {
        return new ResponseEntity<>(noteService.getAllNotesSorted(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNote(@PathVariable("id") Long id) {
        return noteService.getNoteById(id)
                .map(note -> new ResponseEntity<>(note, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private Long getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Long) session.getAttribute("userId");
        }
        return null;
    }
}

