package com.example.knot_server.netty.server.model;

import lombok.Data;

@Data
public class WsCreateMapPost {
    private String type = "MAP_POST_CREATE";
    private String clientReqId; // 客户端请求ID（幂等）

    // 内容
    private String title;
    private String description;
    private String[] mediaUrls; // 已上传的S3 URL数组

    // 位置（必需）
    private Loc loc;

    @Data
    public static class Loc {
        private Double lat;
        private Double lng;
        private String name;
    }

    // 可见范围
    private Boolean allFriends; // true=所有好友, false=指定成员
    private Long[] memberIds; // allFriends=false时使用

    // 帖子类型（可选，默认ALL）
    private String postType = "ALL"; // ALL/REQUEST/COMMENT
}
