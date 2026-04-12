package com.example.HelpNote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "Senha atual é obrigatória.")
    private String currentPassword;

    @NotBlank(message = "Nova senha é obrigatória.")
    @Size(min = 6, message = "Nova senha deve ter no mínimo 6 caracteres.")
    private String newPassword;

    public ChangePasswordRequest() {}

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
