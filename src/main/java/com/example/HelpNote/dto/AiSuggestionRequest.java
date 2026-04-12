package com.example.HelpNote.dto;

public class AiSuggestionRequest {

    private String text;
    private String title;
    private String question;

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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
