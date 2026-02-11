package com.example.HelpNote.controller;

import com.example.HelpNote.domain.Note;
import com.example.HelpNote.dto.AudioUploadRequest;
import com.example.HelpNote.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Note> uploadAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("title") String title) {
        try {
            Note savedNote = noteService.saveAudioFile(audioFile, title);
            return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
        } catch (IOException e) {
            // Log the exception for debugging
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
