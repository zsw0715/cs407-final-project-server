package com.example.knot_server.netty.server.model;

import lombok.Data;

@Data
public class WsMapPostLikeAction {
    private String type = "MAP_POST_LIKE";
    private Long mapPostId;
    private Boolean liked;
    private String clientReqId;
}
