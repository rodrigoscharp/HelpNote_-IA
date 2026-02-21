package com.example.HelpNote.dto;

import java.util.List;

public class AiSuggestionResponse {

    private List<String> keywords;
    private String suggestedCompletion;

    public AiSuggestionResponse() {
    }

    public AiSuggestionResponse(List<String> keywords, String suggestedCompletion) {
        this.keywords = keywords;
        this.suggestedCompletion = suggestedCompletion;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getSuggestedCompletion() {
        return suggestedCompletion;
    }

    public void setSuggestedCompletion(String suggestedCompletion) {
        this.suggestedCompletion = suggestedCompletion;
    }
}
