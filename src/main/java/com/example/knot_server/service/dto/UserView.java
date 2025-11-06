package com.example.knot_server.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserView {
  private Long userid;
  
  private String username;
  
  private String nickname;
  
  private String email;
  
  private String gender;
  
  private Integer age;
  
  private LocalDate birthdate;
  
  private String avatarUrl;
  
  private String accountStatus;
  
  private BigDecimal lastLatitude;
  
  private BigDecimal lastLongitude;
  
  private LocalDateTime lastLocationUpdate;
  
  private String statusMessage;
  
  private LocalDateTime lastOnlineTime;
  
  private Boolean discoverable;
  
  private String privacyLevel;
  
  private String deviceId;
  
  private LocalDateTime createdAt;
}
