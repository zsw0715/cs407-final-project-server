package com.example.knot_server.controller.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MapPostDetailResponse {
    // ========== 帖子基本信息 ==========
    private Long mapPostId;
    private Long convId;  // 会话 ID（前端用这个ID调用 /api/conversation/messages 获取评论）
    private String title;  // 帖子标题
    private String description;  // 帖子描述
    private List<String> mediaUrls;  // 媒体文件 URL 列表
    
    // ========== 作者信息 ==========
    private Long creatorId;
    private String creatorUsername;  // 作者用户名
    private String creatorAvatar;  // 作者头像 URL
    
    // ========== 位置信息 ==========
    private Double locLat;  // 纬度
    private Double locLng;  // 经度
    private String locName;  // 位置名称
    
    // ========== 互动统计 ==========
    private Integer viewCount;  // 浏览数
    private Integer likeCount;  // 点赞数
    private Integer commentCount;  // 评论数
    
    // ========== 时间信息 ==========
    private Long createdAtMs;  // 创建时间戳 (毫秒)
    private String postType;  // 帖子类型 (ALL, REQUEST, etc.)
    
    // ========== 用户交互状态 ==========
    private Boolean isLikedByCurrentUser;  // 当前用户是否点赞了这个帖子
}
