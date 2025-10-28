package com.example.knot_server.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.knot_server.controller.dto.UserSettingsRequest;
import com.example.knot_server.controller.dto.UserSettingsResponse;
import com.example.knot_server.entity.User;
import com.example.knot_server.mapper.UserMapper;
import com.example.knot_server.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public UserSettingsResponse getSettingsByUserId(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      return null;
    }
    UserSettingsResponse response = new UserSettingsResponse();
    response.setNickname(user.getNickname());
    response.setEmail(user.getEmail());
    response.setGender(user.getGender());
    response.setStatusMessage(user.getStatusMessage());
    response.setAvatarUrl(user.getAvatarUrl());
    response.setBirthdate(user.getBirthdate());
    response.setPrivacyLevel(user.getPrivacyLevel());
    response.setDiscoverable(user.getDiscoverable());
    return response;
  }

  @Override
  public UserSettingsResponse updateSettings(Long userId, UserSettingsRequest newSettings) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      return null;
    }

    log.info("Updating user settings for userId " + userId + " with newSettings: " + newSettings);

    // 直接设置所有字段，包括 null 值
    user.setNickname(newSettings.getNickname());
    user.setEmail(newSettings.getEmail());
    user.setGender(newSettings.getGender());
    user.setStatusMessage(newSettings.getStatusMessage());
    user.setAvatarUrl(newSettings.getAvatarUrl());
    user.setBirthdate(newSettings.getBirthdate());
    user.setPrivacyLevel(newSettings.getPrivacyLevel());
    user.setDiscoverable(newSettings.getDiscoverable());

    user.setUpdatedAt(LocalDateTime.now());

    return userMapper.updateById(user) > 0 ? getSettingsByUserId(userId) : null;
  }

  @Override
  public boolean changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      return false;
    }
    String currentPasswordHash = user.getPasswordHash();

    if (!passwordEncoder.matches(oldPassword, currentPasswordHash)) {
      return false;
    }

    String newPasswordHash = passwordEncoder.encode(newPassword);
    user.setPasswordHash(newPasswordHash);
    user.setUpdatedAt(LocalDateTime.now());

    return userMapper.updateById(user) > 0;
  }

}
