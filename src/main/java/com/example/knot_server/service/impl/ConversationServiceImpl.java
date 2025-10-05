package com.example.knot_server.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.knot_server.entity.Conversation;
import com.example.knot_server.entity.ConversationMember;
import com.example.knot_server.entity.SingleConvIndex;
import com.example.knot_server.mapper.ConversationMapper;
import com.example.knot_server.mapper.ConversationMemberMapper;
import com.example.knot_server.mapper.SingleConvIndexMapper;
import com.example.knot_server.service.ConversationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;
    private final SingleConvIndexMapper singleConvIndexMapper;

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

}
