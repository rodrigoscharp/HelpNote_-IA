package com.example.HelpNote.controller;

import java.io.IOException;
import java.util.List;

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

@RestController
@RequestMapping("/api/atas")
public class AtaController {

    private final NoteService noteService;
    private final PdfService pdfService;

    public AtaController(NoteService noteService, PdfService pdfService) {
        this.noteService = noteService;
        this.pdfService = pdfService;
    }

    @PostMapping
    public ResponseEntity<Ata> createAta(
            @RequestParam("title") String title,
            @RequestParam("transcription") String transcription) {
        Ata savedAta = noteService.saveMeetingNote(title, transcription);
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
}
