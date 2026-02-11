package com.example.HelpNote.dto;

import org.springframework.web.multipart.MultipartFile;

public class AudioUploadRequest {
    private MultipartFile audioFile;

    public MultipartFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }
}
