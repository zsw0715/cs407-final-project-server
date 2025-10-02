package com.example.knot_server.util;

import java.io.IOException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// JWS 结构示例
// Jws<Claims> jws = {
//     header: {
//         "alg": "HS256",
//         "typ": "JWT"
//     },
//     body: {                    // 这就是 Claims
//         "sub": "123",          // Subject: 用户ID
//         "username": "szysws", // 自定义字段：用户名
//         "typ": "access",       // 自定义字段：token类型
//         "jti": "uuid-abc-123", // JWT ID：唯一标识符
//         "iat": 1640995200,     // 签发时间
//         "exp": 1640998800      // 过期时间
//     },
//     signature: "..."           // 签名验证
// }

/**
 * JWT 认证过滤器，用于验证每个请求的 JWT 令牌
 * 
 * 主要功能包括：
 * 1. 检查请求头中的 Authorization
 * 2. 解析 JWT，验证签名和过期时间
 * 3. 检查 Redis 黑名单，确保令牌未被撤销
 * 4. 提取用户信息，设置到请求属性中，供后续使用
 * 5. 排除不需要认证的路径，例如 /auth/** 和 /test/**
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final StringRedisTemplate redis;

    /**
     * 过滤每个请求，验证 JWT 令牌
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 放行 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        // 如果没有 Authorization 头，直接返回 401（未授权）
        String h = request.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            response.setStatus(401);
            return;
        }
        try {
            // 解析 JWT, 从 Authorization 头中提取令牌（在第 7 个字符之后（Bearer <token>））
            var jws = jwt.parse(h.substring(7));
            String jti = jws.getBody().getId();
            // 检查 Redis 黑名单中是否存在该 jti, 如果黑名单存在该 jti，说明令牌已被撤销
            if (Boolean.TRUE.equals(redis.hasKey("bl:" + jti))) {
                response.setStatus(401);
                return;
            }
            // 从 JWT 中提取用户信息，设置到 request 中，供后续 Controller 使用
            // 例如在 Controller 中可以通过以下方式获取：
            //     Long userId = (Long) request.getAttribute("uid");                // 123L
            //     String username = (String) request.getAttribute("uname");        // "szysws"
            request.setAttribute("uid", Long.valueOf(jws.getBody().getSubject()));
            request.setAttribute("uname", jws.getBody().get("username", String.class));
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(401);
            return;
        }
    }

    /**
     * 排除不需要过滤的路径，例如 1. /auth/** 2. /test/**
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest req) {
        String path = req.getRequestURI();
        // 与 SecurityConfig 中的放行路径保持一致
        return path.startsWith("/api/auth/") || path.startsWith("/test/") || path.startsWith("/client/") || path.startsWith("/static/");
    }

}
