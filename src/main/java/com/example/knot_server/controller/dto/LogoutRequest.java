package com.example.knot_server.controller.dto;

import lombok.Data;

/**
 * 刷新令牌请求体
 */
@Data
public class LogoutRequest {
    private String username;
}
