package com.example.knot_server.service;

import com.example.knot_server.controller.dto.RegisterResponse;
import com.example.knot_server.controller.dto.TokenResponse;

/**
 * 认证服务接口，
 * 定义用户注册、登录、登出和刷新令牌等方法
 */
public interface AuthService {

    /**
     * 注册新用户
     * @param username    新用户的用户名
     * @param password    新用户的密码
     * @return            注册成功返回用户信息，失败返回错误信息
     */
    RegisterResponse register(String username, String password);

    /**
     * 登录用户
     * @param username    登录用户的用户名
     * @param password    登录用户的密码
     * @return            登录成功返回用户信息，失败返回错误信息
     */
    TokenResponse login(String username, String password);

    /**
     * 用户登出
     * @param username    登出用户的用户名
     * @return            登出成功返回true，失败返回false
     */
    boolean logout(String username);

    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return            刷新成功返回新的访问令牌，失败返回错误信息
     */
    TokenResponse refreshToken(String refreshToken);
}
