package com.example.knot_server.controller.dto;

import lombok.Data;

@Data
public class NearbyPostRequestV2 {
    // 查询中心位置（必填）
    private Double lat;
    private Double lng;
    
    // 搜索半径（米），默认5000米
    private Integer radius = 5000;
    
    // 时间范围，默认7天
    private String timeRange = "7D";  // 1D/7D/30D
    
    // 帖子类型，默认ALL
    private String postType = "ALL";  // ALL/REQUEST/COMMENT
    
    // 最大返回数量（可选，防止过多）
    private Integer maxResults = 200;
}

