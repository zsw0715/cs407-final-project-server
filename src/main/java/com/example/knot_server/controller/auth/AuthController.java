package com.example.knot_server.controller.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.knot_server.controller.auth.dto.ApiResponse;
import com.example.knot_server.controller.auth.dto.RegisterORLoginRequest;
import com.example.knot_server.controller.auth.dto.RegisterResponse;
import com.example.knot_server.controller.auth.dto.TokenResponse;
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
                ApiResponse.success("User registered successfully", response));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody RegisterORLoginRequest request) {
        TokenResponse response = authService.login(request.getUsername(), request.getPassword());
        if (response.getError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(response.getError()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("User logged in successfully", response));
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody String username) {
        boolean result = authService.logout(username);
        if (!result) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error("用户未登录或不存在"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("User logged out successfully", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody String rt) {
        TokenResponse response = authService.refreshToken(rt);
        if (response.getError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(response.getError()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("Token refreshed successfully", response));
    }
}

