package com.example.knot_server.netty.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 新好友申请推送
 * 服务器 → 客户端（接收者）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsFriendRequestNew {
    /** 消息类型 */
    @Builder.Default
    private String type = "FRIEND_REQUEST_NEW";
    
    /** 申请ID */
    private Long requestId;
    
    /** 发送者信息 */
    private UserInfo fromUser;
    
    /** 申请留言 */
    private String message;
    
    /** 申请时间戳 */
    private Long timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String avatarUrl;
    }
}