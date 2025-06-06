package com.small.backend.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdatePasswordRequest {

    @NotBlank
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}