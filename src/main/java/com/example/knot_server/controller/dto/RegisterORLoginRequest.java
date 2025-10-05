package com.example.knot_server.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册或登录请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterORLoginRequest {
    private String username;
    private String password;
}
