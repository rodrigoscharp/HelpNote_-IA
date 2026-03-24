package com.example.HelpNote.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.HelpNote.domain.Ata;
import com.example.HelpNote.service.NoteService;
import com.example.HelpNote.service.PdfService;
import com.example.HelpNote.service.UsageLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/atas")
public class AtaController {

    private final NoteService noteService;
    private final PdfService pdfService;
    private final UsageLimitService usageLimitService;

    public AtaController(NoteService noteService, PdfService pdfService, UsageLimitService usageLimitService) {
        this.noteService = noteService;
        this.pdfService = pdfService;
        this.usageLimitService = usageLimitService;
    }

    @PostMapping
    public ResponseEntity<?> createAta(
            @RequestParam("title") String title,
            @RequestParam("transcription") String transcription,
            HttpServletRequest request) {

        Long userId = getUserIdFromSession(request);
        if (userId != null && !usageLimitService.canCreateAta(userId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Limite diário atingido! No plano gratuito você pode gerar 1 ata por dia.");
            error.put("upgradeRequired", true);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        Ata savedAta = noteService.saveMeetingNote(title, transcription);

        // Increment usage counter
        if (userId != null) {
            usageLimitService.incrementAtaUsage(userId);
        }

        return new ResponseEntity<>(savedAta, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Ata>> getAllAtas() {
        return new ResponseEntity<>(noteService.getAllAtasSorted(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ata> getAta(@PathVariable("id") Long id) {
        return noteService.getAtaById(id)
                .map(ata -> new ResponseEntity<>(ata, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("id") Long id) {
        return noteService.getAtaById(id).map(ata -> {
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
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                String filename = "Ata_" + id + ".pdf";
                headers.setContentDispositionFormData(filename, filename);
                headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
                
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            } catch (IOException e) {
                return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private Long getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Long) session.getAttribute("userId");
        }
        return null;
    }
}

