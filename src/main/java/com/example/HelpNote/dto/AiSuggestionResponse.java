package com.example.HelpNote.dto;

import java.util.List;

public class AiSuggestionResponse {

    private List<String> keywords;
    private String suggestedCompletion;
    private String correctedText;
    private String suggestedTitle;

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

    public String getCorrectedText() {
        return correctedText;
    }

    public void setCorrectedText(String correctedText) {
        this.correctedText = correctedText;
    }

    public String getSuggestedTitle() {
        return suggestedTitle;
    }

    public void setSuggestedTitle(String suggestedTitle) {
        this.suggestedTitle = suggestedTitle;
    }
}
