package com.example.knot_server.service;

import java.util.List;

import com.example.knot_server.controller.dto.ConversationSummaryResponse;

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

}
