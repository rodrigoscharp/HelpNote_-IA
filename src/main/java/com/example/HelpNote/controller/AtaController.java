package com.example.HelpNote.controller;

import com.example.HelpNote.domain.Ata;
import com.example.HelpNote.service.NoteService;
import com.example.HelpNote.service.PdfService;
import com.example.HelpNote.service.UsageLimitService;
import com.example.HelpNote.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        if (!usageLimitService.checkAndIncrementAta(userId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Limite diário atingido! No plano gratuito você pode gerar 2 atas por dia.");
            error.put("upgradeRequired", true);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        Ata savedAta = noteService.saveMeetingNote(title, transcription, userId);
        return new ResponseEntity<>(savedAta, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> getAllAtas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean paged,
            HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        if (paged) {
            Page<Ata> atas = noteService.getAtasPaged(userId, page, size);
            return ResponseEntity.ok(atas);
        }
        List<Ata> atas = noteService.getAllAtasByUser(userId);
        return ResponseEntity.ok(atas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ata> getAta(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return noteService.getAtaById(id, userId)
                .map(ata -> new ResponseEntity<>(ata, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return noteService.getAtaById(id, userId).map(ata -> {
            try {
                String dateStr = ata.getUploadDateTime() != null
                        ? ata.getUploadDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "N/A";
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
}
