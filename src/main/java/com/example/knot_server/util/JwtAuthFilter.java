package com.example.knot_server.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

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
/** 解析 Authorization: Bearer <access token>，把用户放进 SecurityContext */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // 可直接放行的路径（静态资源/登录注册/测试等）
    private static final String[] WHITELIST = {
            "/api/auth/", "/client/", "/static/", "/error", "/test/"
    };

    /** 不需要过滤的路径 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String p : WHITELIST) if (uri.startsWith(p)) return true;
        return false;
    }

    /** 过滤器主逻辑 */
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String auth = req.getHeader("Authorization");
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);

                Claims claims = jwtService.parse(token).getBody(); // 确保 parse 成功返回 Claims
                String typ = claims.get("typ", String.class);
                if (!"access".equalsIgnoreCase(typ)) {
                    throw new RuntimeException("not access token");
                }

                Long uid = Long.valueOf(claims.getSubject()); // sub=uid
                var authentication = new UsernamePasswordAuthenticationToken(
                        new SimplePrincipal(uid), // principal
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")) // 基础角色，避免 403
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext(); // 解析失败当匿名，交由后续鉴权返回 401/403
        }
        chain.doFilter(req, res);
    }

    /** 极简 Principal，只保存 uid。 */
    public record SimplePrincipal(Long uid) {}
}

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