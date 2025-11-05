package com.example.knot_server.netty.server.model;

import lombok.Data;

/**
 * WebSocket 好友申请响应（接受/拒绝）
 * 客户端 → 服务器
 */
@Data
public class WsFriendRequestResponse {
    /** 消息类型：FRIEND_REQUEST_ACCEPT 或 FRIEND_REQUEST_REJECT */
    private String type;
    
    /** 申请ID */
    private Long requestId;
}