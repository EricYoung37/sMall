package com.small.backend.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdatePasswordRequest {

    @NotBlank
    private String newPassword;

    // A valid refresh token should also be required in the request body.
    // Otherwise, after a user logs out (invalidate the refresh token),
    // they may still update the password with the access token if it's not expired.
    private String refreshToken;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}