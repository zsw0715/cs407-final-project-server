package com.example.knot_server.controller.auth.dto;

import lombok.Data;

@Data
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private Long userId;
    private String username;

    public TokenResponse(String accessToken, String refreshToken, long expiresIn, Long userId, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
    }
}
