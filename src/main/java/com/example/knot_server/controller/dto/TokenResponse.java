package com.example.knot_server.controller.dto;

import lombok.Data;

@Data
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String error;  // 用于存储错误信息

    public TokenResponse() {
    }

    public TokenResponse(String accessToken, String refreshToken, Long userId, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
    }

    public TokenResponse(String error) {
        this.error = error;
    }

}
