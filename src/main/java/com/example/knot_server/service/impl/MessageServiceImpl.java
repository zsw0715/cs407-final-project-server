package com.example.knot_server.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.knot_server.entity.Conversation;
import com.example.knot_server.entity.ConversationMember;
import com.example.knot_server.entity.Friends;
import com.example.knot_server.entity.MapPost;
import com.example.knot_server.entity.Message;
import com.example.knot_server.entity.MessageReceipt;
import com.example.knot_server.mapper.ConversationMapper;
import com.example.knot_server.mapper.ConversationMemberMapper;
import com.example.knot_server.mapper.FriendsMapper;
import com.example.knot_server.mapper.MapPostMapper;
import com.example.knot_server.mapper.MessageMapper;
import com.example.knot_server.mapper.MessageReceiptMapper;
import com.example.knot_server.netty.server.model.WsSendMessage;
import com.example.knot_server.service.MessageService;
import com.example.knot_server.service.dto.MessageSavedView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

        private final MessageMapper messageMapper;
        private final ConversationMemberMapper conversationMemberMapper;
        private final ConversationMapper conversationMapper;
        private final MessageReceiptMapper messageReceiptMapper;
        private final FriendsMapper friendsMapper;
        private final MapPostMapper mapPostMapper;

        @Override
        @Transactional(rollbackFor = Exception.class)
        public MessageSavedView saveFromWebSocket(Long senderUid, WsSendMessage req) {
                // 检查参数
                if (req.getConvId() == null || req.getMsgType() == null || req.getClientMsgId() == null) {
                        throw new IllegalArgumentException("Invalid message parameters");
                }

                // 1) 发送者是否会话成员
                boolean isMember = conversationMemberMapper.selectCount(
                                new LambdaQueryWrapper<ConversationMember>()
                                                .eq(ConversationMember::getConvId, req.getConvId())
                                                .eq(ConversationMember::getUserId, senderUid)) > 0;
                if (!isMember) {
                        throw new IllegalArgumentException("Sender is not a member of the conversation");
                }

                // 添加：单聊时检查好友关系
                Conversation conversation = conversationMapper.selectById(req.getConvId());
                if (conversation == null) {
                        throw new IllegalArgumentException("Conversation not found");
                }

                // 如果是单聊（conv_type = 1），必须是好友才能发消息
                if (conversation.getConvType() == 1) {
                        // 获取会话中的所有成员
                        List<ConversationMember> members = conversationMemberMapper.selectList(
                                        new LambdaQueryWrapper<ConversationMember>()
                                                        .eq(ConversationMember::getConvId, req.getConvId()));

                        if (members.size() != 2) {
                                throw new IllegalArgumentException(
                                                "Invalid single conversation: should have exactly 2 members");
                        }

                        // 找到对方的 userId
                        Long otherUserId = members.stream()
                                        .map(ConversationMember::getUserId)
                                        .filter(uid -> !uid.equals(senderUid))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Cannot find the other user in conversation"));

                        // 检查好友关系
                        Long userMin = Math.min(senderUid, otherUserId);
                        Long userMax = Math.max(senderUid, otherUserId);

                        Friends friendship = friendsMapper.selectOne(
                                        new LambdaQueryWrapper<Friends>()
                                                        .eq(Friends::getUserMinId, userMin)
                                                        .eq(Friends::getUserMaxId, userMax)
                                                        .last("LIMIT 1"));

                        if (friendship == null) {
                                throw new IllegalArgumentException("Cannot send message: not friends yet");
                        }
                }
                // 群聊（conv_type = 2）不需要检查好友关系

                // 帖子 (conv_type = 3) 需要更细comment_count
                if (conversation.getConvType() == 3) {
                        // 根据当前会话ID，获取对应的帖子
                        MapPost mapPost = mapPostMapper.selectOne(
                                new LambdaQueryWrapper<MapPost>()
                                        .eq(MapPost::getConvId, conversation.getConvId())
                                        .last("LIMIT 1"));
                        if (mapPost == null) {
                                throw new IllegalArgumentException("Map post not found");
                        }
                        // 更新帖子评论数
                        mapPost.setCommentCount(mapPost.getCommentCount() + 1);
                        mapPostMapper.updateById(mapPost);
                }

                // 2) 幂等：按 (conv_id, client_msg_id)
                Message existed = messageMapper.selectOne(
                                new LambdaQueryWrapper<Message>()
                                                .eq(Message::getConvId, req.getConvId())
                                                .eq(Message::getClientMsgId, req.getClientMsgId())
                                                .last("LIMIT 1"));
                if (existed != null) {
                        // 2.1 命中幂等 → 仍需拿会话成员列表用于广播
                        List<Long> members = conversationMemberMapper.selectList(
                                        new LambdaQueryWrapper<ConversationMember>()
                                                        .eq(ConversationMember::getConvId, req.getConvId()))
                                        .stream()
                                        .map(ConversationMember::getUserId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                        long createdAtMs = (existed.getCreatedAt() != null)
                                        ? existed.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                                                        .toEpochMilli()
                                        : System.currentTimeMillis();

                        return MessageSavedView.builder()
                                        .msgId(existed.getMsgId())
                                        .convId(existed.getConvId())
                                        .fromUid(existed.getSenderId())
                                        .msgType(existed.getMsgType())
                                        .contentText(existed.getContentText())
                                        .mediaUrl(existed.getMediaUrl())
                                        .mediaThumbUrl(existed.getMediaThumbUrl())
                                        .mediaMetaJson(existed.getMediaMetaJson())
                                        .createdAtMs(createdAtMs)
                                        .receiverUids(members)
                                        .build();
                }

                // 3) 新消息入库
                Message msg = new Message();
                msg.setConvId(req.getConvId());
                msg.setSenderId(senderUid);
                msg.setMsgType(req.getMsgType());
                msg.setClientMsgId(req.getClientMsgId());
                msg.setContentText(req.getContentText());
                msg.setMediaUrl(req.getMediaUrl());
                msg.setMediaThumbUrl(req.getMediaThumbUrl());
                msg.setMediaMetaJson(req.getMediaMetaJson());
                if (req.getLoc() != null) {
                        msg.setLocLat(req.getLoc().getLat());
                        msg.setLocLng(req.getLoc().getLng());
                        msg.setLocName(req.getLoc().getName());
                        msg.setLocAccuracyM(req.getLoc().getAccuracy());
                }
                messageMapper.insert(msg);

                // 4) 更新会话 last_msg_id
                conversationMapper.update(null,
                                new LambdaUpdateWrapper<com.example.knot_server.entity.Conversation>()
                                                .eq(com.example.knot_server.entity.Conversation::getConvId,
                                                                req.getConvId())
                                                .set(com.example.knot_server.entity.Conversation::getLastMsgId,
                                                                msg.getMsgId()));

                // 5) 成员列表（用于广播）
                List<Long> members = conversationMemberMapper.selectList(
                                new LambdaQueryWrapper<ConversationMember>()
                                                .eq(ConversationMember::getConvId, req.getConvId()))
                                .stream()
                                .map(ConversationMember::getUserId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                long createdAtMs = (msg.getCreatedAt() != null)
                                ? msg.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                : System.currentTimeMillis();

                // 6) 消息回执，只记录 receivers 的已送达状态
                for (Long uid : members) {
                        if (Objects.equals(uid, senderUid)) {
                                continue; // 发送者不记录已送达
                        }
                        MessageReceipt receipt = new MessageReceipt();
                        LocalDateTime now = LocalDateTime.now();
                        receipt.setMsgId(msg.getMsgId());
                        receipt.setUserId(uid);
                        receipt.setDeliveredAt(now);
                        messageReceiptMapper.insert(receipt);
                }

                // 7) 返回结果
                return MessageSavedView.builder()
                                .msgId(msg.getMsgId())
                                .convId(msg.getConvId())
                                .fromUid(msg.getSenderId())
                                .msgType(msg.getMsgType())
                                .contentText(msg.getContentText())
                                .mediaUrl(msg.getMediaUrl())
                                .mediaThumbUrl(msg.getMediaThumbUrl())
                                .mediaMetaJson(msg.getMediaMetaJson())
                                .createdAtMs(createdAtMs)
                                .receiverUids(members)
                                .build();
        }

}
