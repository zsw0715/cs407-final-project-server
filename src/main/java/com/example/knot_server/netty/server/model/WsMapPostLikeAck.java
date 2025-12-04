package com.example.knot_server.netty.server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsMapPostLikeAck {
    @Builder.Default
    private String type = "MAP_POST_LIKE_ACK";
    private Long mapPostId;
    private Boolean liked;
    private Integer likeCount;
    private String clientReqId;
    private Long serverTime;
}
