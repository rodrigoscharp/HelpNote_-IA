package com.example.HelpNote.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;
import com.example.HelpNote.service.AiService;

@RestController
@RequestMapping("/api/ai")
public class AiNoteController {

    private final AiService aiService;

    public AiNoteController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/suggest")
    public ResponseEntity<AiSuggestionResponse> getSuggestion(@RequestBody AiSuggestionRequest request) {
        AiSuggestionResponse response = aiService.generateSuggestion(request);
        return ResponseEntity.ok(response);
    }
}
