package com.example.knot_server.controller.auth.dto;

import lombok.Data;

@Data
public class RegisterResponse {
    private Long userId;
    private String username;
    private String error;

    public RegisterResponse(String error) {
        this.error = error;
    }

    public RegisterResponse(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

}
