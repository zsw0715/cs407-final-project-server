package com.example.knot_server.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.knot_server.controller.dto.RegisterResponse;
import com.example.knot_server.controller.dto.TokenResponse;
import com.example.knot_server.entity.User;
import com.example.knot_server.mapper.UserMapper;
import com.example.knot_server.service.AuthService;
import com.example.knot_server.util.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final StringRedisTemplate redis;

    @Value("${app.jwt.refresh-days:14}")
    int refreshTokenDays;

    @Transactional
    @Override
    public RegisterResponse register(String username, String password) {
        User userAlreadyExists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (userAlreadyExists != null) {
            return new RegisterResponse("用户已存在");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setNickname(username);
        // 与登录校验一致，使用大写 ACTIVE
        newUser.setAccountStatus("ACTIVE");
        newUser.setDiscoverable(true);
        newUser.setPrivacyLevel("public");
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.insert(newUser);
        return result > 0 ? new RegisterResponse(newUser.getUserid(), newUser.getUsername()) : new RegisterResponse("注册失败");
    }

    @Override
    public TokenResponse login(String username, String password) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (u == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            return new TokenResponse("用户名或密码错误");
        }
        if (!"ACTIVE".equals(u.getAccountStatus())) {
            return new TokenResponse("用户账户被封禁");
        }

        String at = jwt.generateAccessToken(u.getUserid(), u.getUsername());
        String uti = UUID.randomUUID().toString();
        String rt = jwt.generateRefreshToken(u.getUserid(), u.getUsername(), uti);

        // 将用户的 Refresh Token 存储到 Redis，注意只存储 rt！
        // redis: 刷新令牌白名单：rt:{userId} -> jti
        try {
            String key = "rt:" + u.getUserid();
            redis.opsForValue().set(key, uti, Duration.ofDays(refreshTokenDays));
        } catch (Exception e) {
            return new TokenResponse("系统异常，请稍后重试");
        }

        u.setLastOnlineTime(LocalDateTime.now());
        userMapper.updateById(u);

        TokenResponse response = new TokenResponse();
        response.setAccessToken(at);
        response.setRefreshToken(rt);
        // response.setExpiresIn(3600L); // 1 hour 这里不返回过期时间，因为 可以在 application.yml 里配置
        response.setUserId(u.getUserid());
        response.setUsername(u.getUsername());
        return response;
    }

    @Override
    public boolean logout(String username) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        // log.debug("Logging out user: {}", username);
        if (u == null) {
            return false;
        }
        // 删除 Redis 中的 Refresh Token，撤销登录状态
        String key = "rt:" + u.getUserid();
        redis.delete(key);
        return true;
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        try {
            var jws = jwt.parse(refreshToken);

            // 校验 Refresh Token 的类型
            if (!"refresh".equals(jws.getBody().get("typ", String.class))) {
                return new TokenResponse("无效的刷新令牌类型");
            }
            Long userId = Long.valueOf(jws.getBody().getSubject());
            String uti = jws.getBody().getId();

            // 检测 Redis 中是否存在该 jti
            String key = "rt:" + userId;
            String savedUti = redis.opsForValue().get(key);
            if (savedUti == null || !savedUti.equals(uti)) {
                return new TokenResponse("刷新令牌无效或已被撤销");
            }

            // 颁发新的 Access Token
            String at = jwt.generateAccessToken(userId, jws.getBody().get("username", String.class));
            TokenResponse resp = new TokenResponse();
            resp.setAccessToken(at);
            resp.setRefreshToken(refreshToken);
            resp.setUserId(userId);
            resp.setUsername((String) jws.getBody().get("username"));

            return resp;
        } catch (Exception e) {
            return new TokenResponse("刷新令牌无效或已过期");
        }
    }

}
