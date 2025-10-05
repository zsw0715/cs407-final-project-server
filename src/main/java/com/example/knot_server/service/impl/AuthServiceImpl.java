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

/**
 * 认证服务实现类，
 * 处理用户注册、登录、登出和刷新令牌等业务逻辑
 */
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

    /** 用户注册 */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public RegisterResponse register(String username, String password) {
        // 1) 用户名唯一性检查
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (u != null)
            return new RegisterResponse("用户已存在");

        // 2) 创建新用户
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setNickname(username);
        newUser.setAccountStatus("ACTIVE");
        newUser.setDiscoverable(true);
        newUser.setPrivacyLevel("public");
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        // 3) 插入数据库（如果失败需要回滚）, 返回结果
        int result = userMapper.insert(newUser);
        return result > 0 ? new RegisterResponse(newUser.getUserid(), newUser.getUsername())
                : new RegisterResponse("注册失败");
    }

    /** 用户登录 */
    @Override
    public TokenResponse login(String username, String password) {
        // 1) 验证用户名和密码，用户存在且未被封禁
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (u == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            return new TokenResponse("用户名或密码错误");
        }
        if (!"ACTIVE".equals(u.getAccountStatus())) {
            return new TokenResponse("用户账户被封禁");
        }

        // 2) 颁发 Access Token 和 Refresh Token
        // JWT 负载中包含 userId 和 username
        // JWT ID (jti) 用于标识 Refresh Token 的唯一性
        // Access Token 有效期较短（如 15 分钟），Refresh Token 有效期较长（如 14 天）
        // 注意这里的 at 和 rt 都是 JWT 字符串
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

        // 3) 更新用户最后登录时间
        u.setLastOnlineTime(LocalDateTime.now());
        userMapper.updateById(u);

        // 4) 返回结果
        TokenResponse response = new TokenResponse();
        response.setAccessToken(at);
        response.setRefreshToken(rt);
        response.setUserId(u.getUserid());
        response.setUsername(u.getUsername());
        return response;
    }

    /** 用户登出 */
    @Override
    public boolean logout(String username) {
        // 1) 查找用户是否存在
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (u == null) {
            return false;
        }

        // 2) 删除 Redis 中的 Refresh Token，撤销登录状态
        String key = "rt:" + u.getUserid();
        redis.delete(key);
        return true;
    }

    /** 刷新访问令牌 */
    @Override
    public TokenResponse refreshToken(String refreshToken) {
        try {
            // 1) 解析 Refresh Token
            var jws = jwt.parse(refreshToken);

            // 2) 校验 Refresh Token 的类型
            if (!"refresh".equals(jws.getBody().get("typ", String.class))) {
                return new TokenResponse("无效的刷新令牌类型");
            }

            // 3) 提取用户 ID 和 jti
            Long userId = Long.valueOf(jws.getBody().getSubject());
            String uti = jws.getBody().getId();

            // 4) 检测 Redis 中是否存在刚刚提取的 jti
            String key = "rt:" + userId;
            String savedUti = redis.opsForValue().get(key);
            if (savedUti == null || !savedUti.equals(uti)) {
                return new TokenResponse("刷新令牌无效或已被撤销");
            }

            // 5) 如果以上的检测都通过，颁发新的 Access Token
            String at = jwt.generateAccessToken(userId, jws.getBody().get("username", String.class));

            // 6）返回结果
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
