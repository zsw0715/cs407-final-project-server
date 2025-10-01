package com.example.knot_server.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

// JWT 结构示例
// {
//   "sub": "1527",          // Subject: 用户ID
//   "username": "szysws",   // 自定义声明：用户名
//   "typ": "access",        // 自定义声明：令牌类型
//   "jti": "uuid-string",   // JWT ID：唯一标识符
//   "iat": 1640995200,      // Issued At：签发时间
//   "exp": 1640998800       // Expiration：过期时间
// }

/**
 * JWT 服务类，负责生成和解析 JWT 令牌
 */
@Component
public class JwtService {
    private final SecretKey key;
    private final int accessMinutes;
    private final int refreshDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-minutes}") int accessMinutes,
                      @Value("${app.jwt.refresh-days}") int refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    /**
     * 生成 Access Token
     * 
     * @param userId   用户ID
     * @param username 用户名
     * @return         生成的 Access Token
     */
    public String generateAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of("username", username, "typ", "access"))
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    /**
     * 生成 Refresh Token，包含 jti 用于与 Redis 对应
     * 
     * 具体流程：
     *    1. 客户端请求刷新令牌，携带当前的 Refresh Token。
     *    2. 服务端解析并验证 Refresh Token 的有效性。
     *    3. 从 Refresh Token 中提取 jti。并且检查 Redis 中是否存在该 jti。
     *     - 如果存在，说明该 Refresh Token 有效，继续下一步。
     *     - 如果不存在，说明该 Refresh Token 已被撤销或无效，拒绝请求。
     *    4. 生成新的 Access Token 和 Refresh Token，并生成新的 jti，保存到 Redis 中。（旧的 jti 失效）
     *
     * @param userId      用户ID
     * @param username    用户名
     * @param jti         JWT ID
     * @return           生成的 Refresh Token
     */
    public String generateRefreshToken(Long userId, String username, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of("username", username, "typ", "refresh"))
                .setId(jti) // 用 jti 与 Redis 对应
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(refreshDays, ChronoUnit.DAYS)))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    /**
     * 解析并验证 JWT 令牌
     * 
     * @param token JWT 令牌字符串
     * @return 解析后的 Jws<Claims> 对象，包含令牌的声明
     * @throws JwtException 如果令牌无效或过期
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    } 
    
}
