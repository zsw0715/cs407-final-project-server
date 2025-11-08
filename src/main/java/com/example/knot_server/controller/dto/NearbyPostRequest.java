package com.example.knot_server.controller.dto;

import lombok.Data;

@Data
public class NearbyPostRequest {
  // ========== 地图中心位置（必填） ==========
  private Double lat; // 地图中心纬度
  private Double lng; // 地图中心经度
  private String locName; // 位置名称（可选）

  // ========== MapBox视野参数 ==========
  private Integer zoomLevel; // MapBox缩放级别 (0-22)，必填
  private BoundingBox bounds; // 视野边界（可选，提供更精确）

  @Data
  public static class BoundingBox {
    private Double northEastLat; // 东北角纬度
    private Double northEastLng; // 东北角经度
    private Double southWestLat; // 西南角纬度
    private Double southWestLng; // 西南角经度
  }

  // ========== 过滤参数 ==========
  private String timeRange = "7D"; // 时间范围：1D/7D/30D，默认7天
  private String postType = "ALL"; // 帖子类型：ALL/REQUEST/COMMENT，默认ALL

  // ========== 数量控制 ==========
  private Integer maxResults = 100; // 最大返回数量，默认100，防止过载
}