package com.example.knot_server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;

import com.example.knot_server.controller.dto.ApiResponse;
import com.example.knot_server.controller.dto.MapPostDetailResponse;
import com.example.knot_server.controller.dto.MapPostLikeCountResponse;
import com.example.knot_server.controller.dto.NearbyPostRequest;
import com.example.knot_server.controller.dto.NearbyPostRequestV2;
import com.example.knot_server.controller.dto.NearbyPostResponse;
import com.example.knot_server.service.MapPostService;
import com.example.knot_server.util.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mapPost")
@RequiredArgsConstructor
public class MapPostController {
  private final MapPostService mps;

  @PostMapping("/nearby")
  public ResponseEntity<ApiResponse<List<NearbyPostResponse>>> getNearbyPosts(
      @RequestBody NearbyPostRequest req,
      Authentication auth) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    List<NearbyPostResponse> nearbyPosts = mps.getNearbyPosts(req, currentUserId);
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("附近帖子获取成功", nearbyPosts));
  }

  /**
   * V2: 简化版查询附近posts，不使用GeoHash，直接计算距离
   */
  @PostMapping("/v2/nearby")
  public ResponseEntity<ApiResponse<List<NearbyPostResponse>>> getNearbyPostsV2(
      @RequestBody NearbyPostRequestV2 req,
      Authentication auth) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    List<NearbyPostResponse> nearbyPosts = mps.getNearbyPostsV2(req, currentUserId);
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("附近帖子获取成功(V2)", nearbyPosts));
  }

  /**
   * 获取帖子详情使用 mapPostId
   * 返回帖子基本信息、作者信息、统计数据等
   * 评论列表需要前端调用 /api/conversation/messages 接口获取（使用返回的convId）
   */
  @GetMapping("/{mapPostId}")
  public ResponseEntity<ApiResponse<MapPostDetailResponse>> getMapPostDetail(
      @PathVariable Long mapPostId,
      Authentication auth
    ) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    MapPostDetailResponse mapPostDetail = mps.getMapPostDetailByMapPostId(mapPostId, currentUserId);
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("帖子详情获取成功", mapPostDetail));
  }

  /**
   * 获取帖子点赞数
   */
  @GetMapping("/{mapPostId}/likes/count")
  public ResponseEntity<ApiResponse<MapPostLikeCountResponse>> getMapPostLikeCount(
      @PathVariable Long mapPostId,
      Authentication auth
  ) {
    // 确保已通过鉴权
    ((JwtAuthFilter.SimplePrincipal) auth.getPrincipal()).uid();

    int likeCount = mps.getMapPostLikeCount(mapPostId);
    MapPostLikeCountResponse response = new MapPostLikeCountResponse(mapPostId, likeCount);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success("帖子点赞数获取成功", response));
  }

  /**
   * 根据 ID 删除帖子（仅作者可操作）
   */
  @DeleteMapping("/{mapPostId}")
  public ResponseEntity<ApiResponse<Void>> deleteMapPost(
      @PathVariable Long mapPostId,
      Authentication auth
  ) {
    JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
    Long currentUserId = principal.uid();
    mps.deleteMapPost(mapPostId, currentUserId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success("帖子删除成功", null));
  }

}

