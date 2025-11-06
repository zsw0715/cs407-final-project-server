package com.example.knot_server.service;

import com.example.knot_server.controller.dto.UserSettingsRequest;
import com.example.knot_server.controller.dto.UserSettingsResponse;
import com.example.knot_server.service.dto.UserView;

public interface UserService {
  /** 获取用户设置 */
  UserSettingsResponse getSettingsByUserId(Long userId);

  /** 更新用户设置 */
  UserSettingsResponse updateSettings(Long userId, UserSettingsRequest newSettings);

  /** 修改用户密码 */
  boolean changePassword(Long userId, String oldPassword, String newPassword);

  /** 根据用户名获取用户ID */
  UserView getUserByUsername(String username);
}
