package com.example.HelpNote.service;

import com.example.HelpNote.dto.AiSuggestionRequest;
import com.example.HelpNote.dto.AiSuggestionResponse;
import com.example.HelpNote.repository.AtaRepository;
import com.example.HelpNote.repository.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(NoteEnrichmentService.class);

    private final NoteRepository noteRepository;
    private final AtaRepository ataRepository;
    private final AiService aiService;

    public NoteEnrichmentService(NoteRepository noteRepository, AtaRepository ataRepository, AiService aiService) {
        this.noteRepository = noteRepository;
        this.ataRepository = ataRepository;
        this.aiService = aiService;
    }

    @Async("aiTaskExecutor")
    @Transactional
    public void enrichTextNote(Long noteId, String title, String content) {
        try {
            AiSuggestionRequest request = new AiSuggestionRequest();
            request.setTitle(title);
            request.setText(content);
            AiSuggestionResponse suggestion = aiService.generateSuggestion(request);
            if (suggestion != null && suggestion.getKeywords() != null && !suggestion.getKeywords().isEmpty()) {
                noteRepository.findById(noteId).ifPresent(note -> {
                    note.setKeywords(String.join(", ", suggestion.getKeywords()));
                    noteRepository.save(note);
                });
            }
        } catch (Exception e) {
            log.warn("Erro ao enriquecer nota id={}: {}", noteId, e.getMessage());
        }
    }

    @Async("aiTaskExecutor")
    @Transactional
    public void analyzeAudioNote(Long noteId, String transcription) {
        try {
            String analysis = aiService.analyzeTranscript(transcription);
            noteRepository.findById(noteId).ifPresent(note -> {
                note.setSummary(analysis);
                noteRepository.save(note);
            });
        } catch (Exception e) {
            log.warn("Erro ao analisar nota de áudio id={}: {}", noteId, e.getMessage());
        }
    }

    @Async("aiTaskExecutor")
    @Transactional
    public void generateAtaMinutes(Long ataId, String title, String transcription) {
        try {
            String summary = aiService.generateMeetingMinutes(transcription, title);
            ataRepository.findById(ataId).ifPresent(ata -> {
                ata.setSummary(summary);
                ataRepository.save(ata);
            });
        } catch (Exception e) {
            log.warn("Erro ao gerar ata id={}: {}", ataId, e.getMessage());
        }
    }
}
