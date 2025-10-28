package com.example.knot_server.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangePasswordRequest {
  private String oldPassword;
  private String newPassword;
}
