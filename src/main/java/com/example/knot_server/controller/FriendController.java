package com.example.knot_server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.example.knot_server.controller.dto.ApiResponse;
import com.example.knot_server.service.FriendService;
import com.example.knot_server.service.dto.FriendRequestView;
import com.example.knot_server.service.dto.FriendView;
import com.example.knot_server.util.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
  private final FriendService friendService;

  /**
   * 获得所有好友列表
   */
  @GetMapping("/list")
  public ResponseEntity<ApiResponse<?>> listFriends(Authentication auth) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    
    List<FriendView> friends = friendService.listFriends(currentUserId);
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(friends));
  }

  
  /**
   * 获得朋友申请列表
   */
  @GetMapping("/request/list")
  public ResponseEntity<ApiResponse<?>> listFriendRequests(Authentication auth) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    
    List<FriendRequestView> requests = friendService.listFriendRequests(currentUserId);
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(requests));
  }

}
