package com.example.knot_server.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.knot_server.controller.dto.ApiResponse;
import com.example.knot_server.controller.dto.LogoutRequest;
import com.example.knot_server.controller.dto.RefreshTokenRequest;
import com.example.knot_server.controller.dto.RegisterORLoginRequest;
import com.example.knot_server.controller.dto.RegisterResponse;
import com.example.knot_server.controller.dto.TokenResponse;
import com.example.knot_server.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 认证控制器，
 * 处理用户注册、登录、登出和刷新令牌等请求
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    /** 用户注册 */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterORLoginRequest request) {
        RegisterResponse response = authService.register(request.getUsername(), request.getPassword());
        if (response.getError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(response.getError()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("用户注册成功", response));
    }

    /** 用户登录 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody RegisterORLoginRequest request) {
        TokenResponse response = authService.login(request.getUsername(), request.getPassword());
        if (response.getError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(response.getError()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("用户登录成功", response));
    }

    /** 用户登出 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        boolean result = authService.logout(request.getUsername());
        if (!result) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error("用户未登录或不存在"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("用户登出成功", null));
    }

    /** 刷新令牌 rt => at */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest body) {
        String rt = body == null ? null : body.getRt();
        if (rt != null)
            rt = rt.trim();
        TokenResponse response = authService.refreshToken(rt);
        if (response.getError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(response.getError()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("刷新令牌成功（使用 Refresh Token 更换新的 Access Token）", response));
    }
}
