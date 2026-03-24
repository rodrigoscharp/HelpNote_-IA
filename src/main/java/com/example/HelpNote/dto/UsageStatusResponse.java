package com.example.HelpNote.dto;

public class UsageStatusResponse {
    private String planType;
    private int remainingNotes;
    private int remainingAtas;
    private int maxNotes;
    private int maxAtas;
    private boolean premium;

    public UsageStatusResponse() {
    }

    public UsageStatusResponse(String planType, int remainingNotes, int remainingAtas, int maxNotes, int maxAtas, boolean premium) {
        this.planType = planType;
        this.remainingNotes = remainingNotes;
        this.remainingAtas = remainingAtas;
        this.maxNotes = maxNotes;
        this.maxAtas = maxAtas;
        this.premium = premium;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public int getRemainingNotes() {
        return remainingNotes;
    }

    public void setRemainingNotes(int remainingNotes) {
        this.remainingNotes = remainingNotes;
    }

    public int getRemainingAtas() {
        return remainingAtas;
    }

    public void setRemainingAtas(int remainingAtas) {
        this.remainingAtas = remainingAtas;
    }

    public int getMaxNotes() {
        return maxNotes;
    }

    public void setMaxNotes(int maxNotes) {
        this.maxNotes = maxNotes;
    }

    public int getMaxAtas() {
        return maxAtas;
    }

    public void setMaxAtas(int maxAtas) {
        this.maxAtas = maxAtas;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }
}
