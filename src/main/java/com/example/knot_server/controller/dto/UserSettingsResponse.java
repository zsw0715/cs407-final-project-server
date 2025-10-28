package com.example.knot_server.controller.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UserSettingsResponse {
  private String nickname;
  private String email;
  private String gender;
  private String statusMessage;
  private String avatarUrl;
  private LocalDate birthdate;
  private String privacyLevel;
  private Boolean discoverable; 
}
