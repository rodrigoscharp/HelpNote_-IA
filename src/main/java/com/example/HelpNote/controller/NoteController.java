package com.example.HelpNote.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.HelpNote.domain.Note;
import com.example.HelpNote.domain.Ata;
import com.example.HelpNote.service.NoteService;
import com.example.HelpNote.service.PdfService;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final PdfService pdfService;

    public NoteController(NoteService noteService, PdfService pdfService) {
        this.noteService = noteService;
        this.pdfService = pdfService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Note> uploadAudio(
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
    public ResponseEntity<Note> createNote(
            @RequestParam("title") String title,
            @RequestParam("content") String content) {
        Note savedNote = noteService.saveTextNote(title, content);
        return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
    }

    @PostMapping("/meeting")
    public ResponseEntity<Ata> saveMeeting(
            @RequestParam("title") String title,
            @RequestParam("transcription") String transcription) {
        Ata savedAta = noteService.saveMeetingNote(title, transcription);
        return new ResponseEntity<>(savedAta, HttpStatus.CREATED);
    }

    @org.springframework.web.bind.annotation.GetMapping("/atas")
    public ResponseEntity<java.util.List<Ata>> getAllAtas() {
        return new ResponseEntity<>(noteService.getAllAtasSorted(), HttpStatus.OK);
    }

    @org.springframework.web.bind.annotation.GetMapping("/atas/{id}")
    public ResponseEntity<Ata> getAta(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        return noteService.getAtaById(id)
                .map(ata -> new ResponseEntity<>(ata, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<java.util.List<Note>> getAllNotes() {
        return new ResponseEntity<>(noteService.getAllNotesSorted(), HttpStatus.OK);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public ResponseEntity<Note> getNote(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        return noteService.getNoteById(id)
                .map(note -> new ResponseEntity<>(note, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        // Try finding as Ata first
        java.util.Optional<Ata> ataOpt = noteService.getAtaById(id);
        if (ataOpt.isPresent()) {
            Ata ata = ataOpt.get();
            try {
                String dateStr = ata.getUploadDateTime() != null ? 
                    ata.getUploadDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                
                String html = pdfService.generateAtaHtml(
                    ata.getTitle(), 
                    dateStr, 
                    ata.getTranscription() != null ? ata.getTranscription() : "Sem transcrição disponível.",
                    ata.getSummary()
                );
                
                byte[] pdfBytes = pdfService.generatePdfFromHtml(html);
                return createPdfResponse(pdfBytes, id);
            } catch (IOException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Fallback or handle regular Notes if needed (though Meeting Minutes are now Atas)
        return noteService.getNoteById(id).map(note -> {
            try {
                String dateStr = note.getUploadDateTime() != null ? 
                    note.getUploadDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                
                String html = pdfService.generateAtaHtml(
                    note.getTitle(), 
                    dateStr, 
                    note.getTranscription() != null ? note.getTranscription() : "Sem transcrição disponível.",
                    note.getSummary()
                );
                
                byte[] pdfBytes = pdfService.generatePdfFromHtml(html);
                return createPdfResponse(pdfBytes, id);
            } catch (IOException e) {
                return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfBytes, Long id) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        String filename = "Ata_" + id + ".pdf";
        headers.setContentDispositionFormData(filename, filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
