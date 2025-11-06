package com.example.knot_server.netty.server.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsMapPostAck {
    @Builder.Default
    private String type = "MAP_POST_ACK";
    private String clientReqId;      // 回显客户端请求ID
    private Long mapPostId;          // 创建的Map Post ID
    private Long convId;             // 评论区ID
    private Long serverTime;         // 服务器时间戳
}