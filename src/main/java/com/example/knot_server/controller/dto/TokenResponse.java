package com.example.knot_server.controller.dto;

import lombok.Data;

/**
 * 令牌响应 DTO，
 * 包含访问令牌、刷新令牌及用户信息
 */
@Data
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String error;  // 用于存储错误信息

    /** 默认构造函数 */
    public TokenResponse() {
    }

    /** 成功响应构造函数 */
    public TokenResponse(String accessToken, String refreshToken, Long userId, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
    }

    /** 错误响应构造函数 */
    public TokenResponse(String error) {
        this.error = error;
    }
}
