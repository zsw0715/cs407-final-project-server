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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 掌管用户注册、登录、登出、token刷新等功能
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

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

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest body) {
        String rt = body == null ? null : body.getRt();
        if (rt != null) rt = rt.trim();
        TokenResponse response = authService.refreshToken(rt);
        if (response.getError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(response.getError()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("刷新令牌成功（使用 Refresh Token " + rt + " 更换新的 Access Token）", response));
    }
}
