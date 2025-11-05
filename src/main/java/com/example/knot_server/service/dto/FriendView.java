package com.example.knot_server.service.dto;

import lombok.Builder;
import lombok.Data;

// 创建 FriendView.java
@Data
@Builder
public class FriendView {
    /** 好友用户ID */
    private Long friendId;
    
    /** 好友用户名 */
    private String username;
    
    /** 好友头像 */
    private String avatar;
    
    /** 成为好友的时间戳（毫秒） */
    private Long createdAtMs;
    
    /** 单聊会话ID（可选） */
    private Long convId;
}