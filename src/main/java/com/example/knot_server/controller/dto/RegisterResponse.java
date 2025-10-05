package com.example.knot_server.controller.dto;

import lombok.Data;

/**
 * 用户注册响应 DTO
 */
@Data
public class RegisterResponse {
    private Long userId;
    private String username;
    private String error;

    /** 错误响应构造函数 */
    public RegisterResponse(String error) {
        this.error = error;
    }

    /** 成功响应构造函数 */
    public RegisterResponse(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
