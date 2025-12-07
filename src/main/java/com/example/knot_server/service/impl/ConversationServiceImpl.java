package com.example.knot_server.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.example.knot_server.controller.dto.ConversationSummaryResponse;
import com.example.knot_server.controller.dto.MessagePageResponse;
import com.example.knot_server.controller.dto.UserSettingsResponse;
import com.example.knot_server.entity.Conversation;
import com.example.knot_server.entity.ConversationMember;
import com.example.knot_server.entity.Message;
import com.example.knot_server.entity.SingleConvIndex;
import com.example.knot_server.mapper.ConversationMapper;
import com.example.knot_server.mapper.ConversationMemberMapper;
import com.example.knot_server.mapper.SingleConvIndexMapper;
import com.example.knot_server.mapper.UserMapper;
import com.example.knot_server.service.ConversationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;
    private final SingleConvIndexMapper singleConvIndexMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreateSingleConv(Long uidA, Long uidB) {
        Long u1 = Math.min(uidA, uidB);
        Long u2 = Math.max(uidA, uidB);

        // 1) 先查是否已存在
        SingleConvIndex exist = singleConvIndexMapper.selectOne(
                new LambdaQueryWrapper<SingleConvIndex>()
                        .eq(SingleConvIndex::getUserMinId, u1)
                        .eq(SingleConvIndex::getUserMaxId, u2)
                        .last("LIMIT 1"));
        if (exist != null) {
            return exist.getConvId();
        }

        // 2) 不存在则新建（手动填充时间，避免 created_at 为空）
        var now = java.time.LocalDateTime.now();

        Conversation conv = new Conversation();
        conv.setConvType(1);
        conv.setCreatorId(uidA);
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);
        conversationMapper.insert(conv);

        // 3) 建立唯一索引 (u1,u2) -> convId
        SingleConvIndex idx = new SingleConvIndex();
        idx.setUserMinId(u1);
        idx.setUserMaxId(u2);
        idx.setConvId(conv.getConvId());
        try {
            singleConvIndexMapper.insert(idx);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发下可能别的请求已插入，重查拿回 convId
            SingleConvIndex again = singleConvIndexMapper.selectOne(
                    new LambdaQueryWrapper<SingleConvIndex>()
                            .eq(SingleConvIndex::getUserMinId, u1)
                            .eq(SingleConvIndex::getUserMaxId, u2)
                            .last("LIMIT 1"));
            return again.getConvId();
        }

        // 4) 建立成员（同样手动填充 joined_at）
        ConversationMember m1 = new ConversationMember();
        m1.setConvId(conv.getConvId());
        m1.setUserId(uidA);
        m1.setJoinedAt(now);
        conversationMemberMapper.insert(m1);

        ConversationMember m2 = new ConversationMember();
        m2.setConvId(conv.getConvId());
        m2.setUserId(uidB);
        m2.setJoinedAt(now);
        conversationMemberMapper.insert(m2);

        return conv.getConvId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroupConv(String groupName, Long[] memberIds, Long creatorId) {
        var now = java.time.LocalDateTime.now();

        // 1) 创建 conversation 记录, mysql, convType=2 (group_conversation)
        Conversation conv = new Conversation();
        conv.setConvType(2);
        conv.setTitle(groupName);
        conv.setCreatorId(creatorId);
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);
        conversationMapper.insert(conv);

        // 2) 创建 conversation_member 记录
        for (Long memberId : memberIds) {
            ConversationMember member = new ConversationMember();
            member.setConvId(conv.getConvId());
            member.setUserId(memberId);
            member.setJoinedAt(now);
            member.setConvRole(0); // 0=普通成员
            conversationMemberMapper.insert(member);
        }

        // 3） 创建者作为群主加入
        ConversationMember owner = new ConversationMember();
        owner.setConvId(conv.getConvId());
        owner.setUserId(creatorId);
        owner.setJoinedAt(now);
        owner.setConvRole(2); // 2=群主
        conversationMemberMapper.insert(owner);

        return conv.getConvId();
    }

    @Override
    public List<ConversationSummaryResponse> listUserConversations(Long userId) {
        List<ConversationSummaryResponse> conversations = conversationMapper.listUserConversations(userId);
        return conversations;
    }

    @Override
    public MessagePageResponse getConversationMessages(Long conversationId, int page, int size) {
        // 1. 计算 offset
        int offset = (page - 1) * size;
        
        // 2. 查询消息列表
        List<Message> messages = conversationMapper.getConversationMessages(conversationId, size, offset);
        
        // 3. 查询总记录数
        Long total = conversationMapper.countConversationMessages(conversationId);
        
        // 4. 计算总页数
        int totalPages = (int) Math.ceil((double) total / size);
        
        // 5. 构建返回对象
        MessagePageResponse response = new MessagePageResponse();
        response.setConvId(conversationId);
        response.setPage(page);
        response.setSize(size);
        response.setTotal(total);
        response.setTotalPages(totalPages);
        response.setMessageList(messages);
        
        return response;
    }

    @Override
    public boolean isMember(Long conversationId, Long userId) {
        return conversationMemberMapper.selectCount(
                new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getConvId, conversationId)
                        .eq(ConversationMember::getUserId, userId)
                        .last("LIMIT 1")) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserToGroup(Long conversationId, Long userId) {
        // 1) 确认会话存在且为群聊
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (conversation.getConvType() != 2) {
            throw new IllegalArgumentException("不是群聊会话");
        }

        // 2) 已在群里则直接返回
        if (isMember(conversationId, userId)) {
            return;
        }

        // 3) 插入群成员记录
        ConversationMember member = new ConversationMember();
        member.setConvId(conversationId);
        member.setUserId(userId);
        member.setConvRole(0);
        member.setJoinedAt(LocalDateTime.now());
        conversationMemberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserFromGroup(Long conversationId, Long userId) {
        conversationMemberMapper.delete(
                new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getConvId, conversationId)
                        .eq(ConversationMember::getUserId, userId)
                        .last("LIMIT 1"));
    }

    /**
     * 只能获取 convType == 1 or convType == 2 的会话成员列表
     */
    @Override
    public List<UserSettingsResponse> listGroupMembers(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (conversation.getConvType() != 1 && conversation.getConvType() != 2) {
            throw new IllegalArgumentException("不是单聊或群聊会话");
        }

        long[] userIds = conversationMemberMapper.selectList(
                new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getConvId, conversationId))
                .stream()
                .map(ConversationMember::getUserId)
                .mapToLong(Long::longValue)
                .toArray();

        if (userIds.length == 0) {
            return List.of();
        }

        // 批量查询用户信息
        List<Long> userIdList = java.util.Arrays.stream(userIds).boxed().collect(Collectors.toList());
        return userMapper.selectBatchIds(userIdList).stream().map(user -> {
            UserSettingsResponse resp = new UserSettingsResponse();
            resp.setNickname(user.getNickname());
            resp.setEmail(user.getEmail());
            resp.setGender(user.getGender());
            resp.setStatusMessage(user.getStatusMessage());
            resp.setAvatarUrl(user.getAvatarUrl());
            resp.setBirthdate(user.getBirthdate());
            resp.setPrivacyLevel(user.getPrivacyLevel());
            resp.setDiscoverable(user.getDiscoverable());
            return resp;
        }).toList();
    }

}
