package com.example.knot_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserSettingsController {
  
  // 我来写这个 -- zsw （忘记切branch了）
  @GetMapping("/settings")
  public ResponseEntity<?> getUserSettings(Long userId) {
    // TODO: Implement the logic to retrieve user settings
    return null;
  }

}
