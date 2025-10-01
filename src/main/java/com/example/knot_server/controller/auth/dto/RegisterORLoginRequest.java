package com.example.knot_server.controller.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterORLoginRequest {
    private String username;
    private String password;
}
