package com.example.knot_server.service;

/**
 * 会话服务接口
 */
public interface ConversationService {
    Long getOrCreateSingleConv(Long uidA, Long uidB);

    Long createGroupConv(String groupName, Long[] memberIds, Long creatorId);
}
