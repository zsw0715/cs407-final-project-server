package com.example.knot_server.netty.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 好友请求操作确认
 * 服务器 → 客户端（操作者）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsFriendRequestAck {
    /** 消息类型 */
    @Builder.Default
    private String type = "FRIEND_REQUEST_ACK";
    
    /** 申请ID */
    private Long requestId;
    
    /** 操作状态：sent/accepted/rejected */
    private String status;
    
    /** 会话ID（仅在 accepted 时返回） */
    private Long convId;
    
    /** 时间戳 */
    private Long timestamp;
}