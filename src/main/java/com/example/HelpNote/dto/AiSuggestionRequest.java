package com.example.HelpNote.dto;

public class AiSuggestionRequest {

    private String text;
    private String title;

    public AiSuggestionRequest() {
    }

    public AiSuggestionRequest(String text, String title) {
        this.text = text;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
