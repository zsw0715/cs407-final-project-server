package com.example.knot_server.service;

import java.util.List;

import com.example.knot_server.controller.dto.ConversationSummaryResponse;
import com.example.knot_server.controller.dto.MessagePageResponse;
import com.example.knot_server.controller.dto.UserSettingsResponse;

/**
 * 会话服务接口
 */
public interface ConversationService {

    /**
     * 获取或创建单聊会话
     */
    Long getOrCreateSingleConv(Long uidA, Long uidB);

    /**
     * 创建群聊会话
     */
    Long createGroupConv(String groupName, Long[] memberIds, Long creatorId);

    /**
     * 获取用户的所有会话
     */
    List<ConversationSummaryResponse> listUserConversations(Long userId);

    /**
     * 分页获取会话中 messages
     */
    MessagePageResponse getConversationMessages(Long conversationId, int page, int size);

    /**
     * 检查当前用户是否是会话的成员
     */
    boolean isMember(Long conversationId, Long userId);

    /**
     * 将用户添加到群聊会话
     */
    void addUserToGroup(Long conversationId, Long userId);

    /**
     * 将用户从群聊会话中移除
     */
    void removeUserFromGroup(Long conversationId, Long userId);

    /**
     * 获取群聊中所有的成员列表
     */
    List<UserSettingsResponse> listGroupMembers(Long conversationId);
}
