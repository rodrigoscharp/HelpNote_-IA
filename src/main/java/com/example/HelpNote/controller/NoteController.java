package com.example.HelpNote.controller;

import com.example.HelpNote.domain.Note;
import com.example.HelpNote.service.NoteService;
import com.example.HelpNote.service.PdfService;
import com.example.HelpNote.service.UsageLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final UsageLimitService usageLimitService;
    private final PdfService pdfService;

    public NoteController(NoteService noteService, UsageLimitService usageLimitService, PdfService pdfService) {
        this.noteService = noteService;
        this.usageLimitService = usageLimitService;
        this.pdfService = pdfService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("title") String title,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        try {
            Note savedNote = noteService.saveAudioFile(audioFile, title, userId);
            return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao processar arquivo de áudio."));
        }
    }

    @PostMapping
    public ResponseEntity<?> createNote(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();

        if (!usageLimitService.canCreateNote(userId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Limite diário atingido! No plano gratuito você pode criar 1 anotação inteligente por dia.");
            error.put("upgradeRequired", true);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        Note savedNote = noteService.saveTextNote(title, content, userId);
        usageLimitService.incrementNoteUsage(userId);
        return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> getAllNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean paged,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();

        if (paged) {
            Page<Note> notes = noteService.getNotesPaged(userId, page, size);
            return ResponseEntity.ok(notes);
        }
        List<Note> notes = noteService.getAllNotesByUser(userId);
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNote(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return noteService.getNoteById(id, userId)
                .map(note -> new ResponseEntity<>(note, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        return noteService.updateNote(id, title, content, userId)
                .<ResponseEntity<?>>map(note -> ResponseEntity.ok(note))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        boolean deleted = noteService.deleteNote(id, userId);
        if (deleted) return ResponseEntity.ok(Map.of("message", "Nota excluída com sucesso."));
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return noteService.getNoteById(id, userId).map(note -> {
            try {
                String dateStr = note.getUploadDateTime() != null
                        ? note.getUploadDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "N/A";
                String html = pdfService.generateNoteHtml(note.getTitle(), dateStr, note.getContent(), note.getKeywords());
                byte[] pdfBytes = pdfService.generatePdfFromHtml(html);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                String filename = "Nota_" + id + ".pdf";
                headers.setContentDispositionFormData(filename, filename);
                headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            } catch (IOException e) {
                return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Long) session.getAttribute("userId");
        }
        return null;
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado."));
    }
}
