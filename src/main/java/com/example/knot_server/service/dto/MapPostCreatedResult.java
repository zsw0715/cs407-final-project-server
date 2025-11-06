package com.example.knot_server.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MapPostCreatedResult {
    private Long mapPostId;           // 创建的Map Post ID
    private Long convId;              // 评论区ID
    private Long creatorId;           // 创建者ID
    private String creatorUsername;   // 创建者用户名
    private String title;
    private String description;
    private String[] mediaUrls;
    private Double locLat;
    private Double locLng;
    private String locName;
    private Long createdAtMs;         // 创建时间戳（毫秒）
    private List<Long> memberIds;     // 可见成员ID列表（用于推送）
}