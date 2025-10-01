package com.example.knot_server.controller.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.knot_server.controller.auth.dto.ApiResponse;
import com.example.knot_server.controller.auth.dto.RegisterRequest;
import com.example.knot_server.controller.auth.dto.TokenResponse;
import com.example.knot_server.entity.User;

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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@RequestBody RegisterRequest request) {
        // Placeholder implementation
        TokenResponse tokenResponse = new TokenResponse(
            "dummy-access-token",
            "dummy-refresh-token", 
            3600L,
            1L,
            request.getUsername()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("User registered successfully", tokenResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody RegisterRequest request) {
        // Placeholder implementation
        TokenResponse tokenResponse = new TokenResponse(
            "dummy-access-token",
            "dummy-refresh-token", 
            3600L,
            1L,
            request.getUsername()
        );
        
        return ResponseEntity.ok(ApiResponse.success("User logged in successfully", tokenResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Placeholder implementation
        return ResponseEntity.ok(ApiResponse.success("User logged out successfully", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody TokenResponse tokenResponse) {
        // Placeholder implementation
        TokenResponse newTokenResponse = new TokenResponse(
            "new-dummy-access-token",
            "new-dummy-refresh-token",
            3600L,
            1L,
            "dummy-username"
        );

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", newTokenResponse));
    }
}

