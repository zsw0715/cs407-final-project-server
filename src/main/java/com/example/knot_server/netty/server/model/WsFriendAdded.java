package com.example.knot_server.netty.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 好友添加成功通知
 * 服务器 → 客户端（申请发起者）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsFriendAdded {
    /** 消息类型 */
    @Builder.Default
    private String type = "FRIEND_ADDED";
    
    /** 申请ID */
    private Long requestId;
    
    /** 好友信息 */
    private FriendInfo friend;
    
    /** 单聊会话ID */
    private Long convId;
    
    /** 时间戳 */
    private Long timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendInfo {
        private Long userId;
        private String username;
        private String avatarUrl;
    }
}