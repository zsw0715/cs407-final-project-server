package com.example.knot_server.netty.server.model;

import lombok.Data;

/**
 * WebSocket 发送好友申请请求
 * 客户端 → 服务器 
 */
@Data
public class WsFriendRequestSend {
    /** 消息类型 */
    private String type = "FRIEND_REQUEST_SEND";
    
    /** 接收者用户ID */
    private Long receiverId;
    
    /** 申请留言 */
    private String message;
}