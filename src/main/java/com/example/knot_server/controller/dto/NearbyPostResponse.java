package com.example.knot_server.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NearbyPostResponse {
  // ========== Map Post 基本信息 ==========
  private Long mapPostId;
  private Long convId; // 评论区ID
  private String title;
  private String description;
  private String[] mediaUrls; // 媒体URL数组

  // ========== 位置信息 ==========
  private Double locLat; // 纬度（前端用于渲染marker）
  private Double locLng; // 经度（前端用于渲染marker）
  private String locName; // 位置名称
  private Double distance; // 距离用户的距离（米）

  // ========== 创建者信息 ==========
  private Long creatorId;
  private String creatorUsername;
  private String creatorAvatar; // 可选

  // ========== 统计信息 ==========
  private Integer viewCount;
  private Integer likeCount;
  private Integer commentCount;

  // ========== 帖子类型 ==========
  private String postType; // ALL/REQUEST/COMMENT

  // ========== 时间信息 ==========
  private Long createdAtMs; // 创建时间戳（毫秒）
}