package com.example.knot_server.netty.server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsMapPostLikeUpdate {
    @Builder.Default
    private String type = "MAP_POST_LIKE_UPDATE";
    private Long mapPostId;
    private Long userId;
    private Boolean liked;
    private Integer likeCount;
    private Long serverTime;
}
