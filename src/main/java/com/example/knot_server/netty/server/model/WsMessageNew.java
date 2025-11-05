package com.example.knot_server.netty.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 新消息推送
 * 服务器推送新消息给会话成员
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 空字段不序列化
public class WsMessageNew {
    /** 消息类型 */
    @Builder.Default
    private String type = "MSG_NEW";
    
    /** 会话ID */
    private Long convId;
    
    /** 消息ID */
    private Long msgId;
    
    /** 发送者用户ID */
    private Long fromUid;
    
    /** 消息类型：0=TEXT,1=IMAGE,2=VIDEO,3=LOCATION,4=STICKER,5=SYSTEM */
    private Integer msgType;
    
    /** 文本内容 */
    private String contentText;
    
    /** 媒体URL */
    private String mediaUrl;
    
    /** 媒体缩略图URL */
    private String mediaThumbUrl;
    
    /** 媒体元信息JSON（会被解析为对象） */
    private Object mediaMeta;
    
    /** 消息创建时间戳（毫秒） */
    private Long createdAtMs;
}

