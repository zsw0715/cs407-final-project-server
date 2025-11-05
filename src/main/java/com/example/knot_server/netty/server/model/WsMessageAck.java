package com.example.knot_server.netty.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息确认响应
 * 服务器确认收到客户端发送的消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessageAck {
    /** 消息类型 */
    @Builder.Default
    private String type = "MSG_ACK";
    
    /** 服务器生成的消息ID */
    private Long msgId;
    
    /** 客户端消息ID（用于客户端关联请求） */
    private String clientMsgId;
    
    /** 服务器时间戳 */
    private Long serverTime;
}

