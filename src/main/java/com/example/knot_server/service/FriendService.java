package com.example.knot_server.service;

import java.util.List;

import com.example.knot_server.service.dto.FriendRequestView;
import com.example.knot_server.service.dto.FriendView;

/**
 * 好友服务接口
 */
public interface FriendService {
    
    /**
     * 发送好友申请
     * @param requesterId 申请者ID
     * @param receiverId 接收者ID
     * @param message 申请留言
     * @return 好友申请视图
     */
    FriendRequestView sendFriendRequest(Long requesterId, Long receiverId, String message);
    
    /**
     * 接受好友申请
     * @param requestId 申请ID
     * @param currentUserId 当前用户ID（必须是接收者）
     * @return 好友申请视图（包含 convId）
     */
    FriendRequestView acceptFriendRequest(Long requestId, Long currentUserId);
    
    /**
     * 拒绝好友申请
     * @param requestId 申请ID
     * @param currentUserId 当前用户ID（必须是接收者）
     * @return 好友申请视图
     */
    FriendRequestView rejectFriendRequest(Long requestId, Long currentUserId);

    /**
     * 获得所有好友列表
     * @param currentUserId 当前用户ID
     * @return 好友列表
     */
    List<FriendView> listFriends(Long currentUserId);

    /**
     * 获得朋友申请列表
     * @param currentUserId 当前用户ID
     * @return 朋友申请列表
     */
    List<FriendRequestView> listFriendRequests(Long currentUserId);
}