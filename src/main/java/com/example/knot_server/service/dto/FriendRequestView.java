package com.example.knot_server.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友申请视图
 * Service 层返回给 Handler 层的数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestView {
    /** 申请ID */
    private Long requestId;
    
    /** 申请者用户ID */
    private Long requesterId;
    
    /** 接收者用户ID */
    private Long receiverId;
    
    /** 申请留言 */
    private String message;
    
    /** 申请状态：0=待处理,1=已接受,2=已拒绝 */
    private Integer status;
    
    /** 创建时间戳（毫秒） */
    private Long createdAtMs;
    
    /** 会话ID（仅在接受后有值） */
    private Long convId;
}