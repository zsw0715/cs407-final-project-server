package com.example.knot_server.controller.test;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {
  @GetMapping("/me")
  public Map<String, Object> me(Authentication auth) {
    // 如果你的 JwtAuthFilter 把 userId 放在 Principal 或 Details 里，这里取出来
    return Map.of(
      "authenticated", auth != null && auth.isAuthenticated(),
      "principal", auth != null ? auth.getPrincipal() : null,
      "authorities", auth != null ? auth.getAuthorities() : List.of()
    );
  }
}