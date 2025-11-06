package com.example.knot_server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.knot_server.controller.dto.ApiResponse;
import com.example.knot_server.controller.dto.ChangePasswordRequest;
import com.example.knot_server.controller.dto.UserSettingsRequest;
import com.example.knot_server.controller.dto.UserSettingsResponse;
import com.example.knot_server.service.UserService;
import com.example.knot_server.service.dto.UserView;
import com.example.knot_server.util.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserManageController {

  private final UserService userService;

  /**
   * 获取用户设置
   * 
   * @param userId 用户ID
   * @return 用户设置响应
   */
  @GetMapping("/settings")
  public ResponseEntity<ApiResponse<UserSettingsResponse>> getUserSettings(Authentication auth) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    UserSettingsResponse settings = userService.getSettingsByUserId(currentUserId);
    return settings != null
        ? ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("用户设置获取成功", settings))
        : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("无法获取用户设置"));
  }

  /**
   * 更新用户设置
   * 
   * @param userId   用户ID
   * @param settings 新的用户设置
   * @return 更新后的用户设置响应
   */
  @PutMapping("/settings")
  public ResponseEntity<ApiResponse<UserSettingsResponse>> updateUserSettings(
      @RequestBody UserSettingsRequest newSettings,
      Authentication auth) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    UserSettingsResponse updatedSettings = userService.updateSettings(currentUserId, newSettings);
    return updatedSettings != null
        ? ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("用户设置更新成功", updatedSettings))
        : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("无法更新用户设置"));
  }

  /**
   * 修改用户密码
   * 
   * @param userId      用户ID
   * @param oldPassword 旧密码
   * @param newPassword 新密码
   * @return 修改结果响应
   */
  @PutMapping("/changePassword")
  public ResponseEntity<ApiResponse<?>> changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    boolean result = userService.changePassword(currentUserId, request.getOldPassword(), request.getNewPassword());
    return result
        ? ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("密码修改成功"))
        : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("旧密码不正确，无法修改密码"));
  }

  /**
   * 根据用户名获取用户信息
   * 
   * @param username 用户名
   * @return 用户信息响应
   */
  @GetMapping("/info")
  public ResponseEntity<ApiResponse<UserView>> getUserByUsername(@RequestParam String username) {
    UserView user = userService.getUserByUsername(username);
    return user != null
        ? ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("用户信息获取成功", user))
        : ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("用户不存在", null));
  }
}
