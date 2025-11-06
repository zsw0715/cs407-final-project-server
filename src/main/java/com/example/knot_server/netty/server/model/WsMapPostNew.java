package com.example.knot_server.netty.server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsMapPostNew {
    @Builder.Default
    private String type = "MAP_POST_NEW";
    private Long mapPostId;
    private Long convId;             // 评论区ID
    private Long creatorId;          // 创建者ID
    private String creatorUsername;  // 创建者用户名（避免客户端再查）
    private String title;
    private String description;
    private String[] mediaUrls;
    private Loc loc;
    private Long createdAtMs;        // 创建时间戳
    
    @Data
    @Builder
    public static class Loc {
        private Double lat;
        private Double lng;
        private String name;
    }
}