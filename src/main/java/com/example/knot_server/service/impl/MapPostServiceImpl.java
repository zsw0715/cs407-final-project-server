package com.example.knot_server.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.knot_server.entity.Conversation;
import com.example.knot_server.entity.ConversationMember;
import com.example.knot_server.entity.MapPost;
import com.example.knot_server.entity.User;
import com.example.knot_server.mapper.ConversationMapper;
import com.example.knot_server.mapper.ConversationMemberMapper;
import com.example.knot_server.mapper.MapPostMapper;
import com.example.knot_server.mapper.UserMapper;
import com.example.knot_server.netty.server.model.WsCreateMapPost;
import com.example.knot_server.service.FriendService;
import com.example.knot_server.service.MapPostService;
import com.example.knot_server.service.dto.FriendView;
import com.example.knot_server.service.dto.MapPostCreatedResult;
import com.example.knot_server.util.GeoHashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapPostServiceImpl implements MapPostService {
  private final MapPostMapper mapPostMapper;
  private final ConversationMapper conversationMapper;
  private final ConversationMemberMapper conversationMemberMapper;
  private final UserMapper userMapper;
  private final ObjectMapper objectMapper;
  private final FriendService friendService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public MapPostCreatedResult createFromWebSocket(Long creatorId, WsCreateMapPost req) {
    LocalDateTime now = LocalDateTime.now();

    // 1. 参数校验
    validateRequest(req);

    // 2. 确定可见成员列表
    List<Long> memberIds = determineMemberIds(creatorId, req);

    // 3. 创建 conversation (评论区)
    Conversation conv = new Conversation();
    conv.setConvType(3); // Map Post评论区
    conv.setTitle(req.getTitle());
    conv.setCreatorId(creatorId);
    conv.setCreatedAt(now);
    conv.setUpdatedAt(now);
    conversationMapper.insert(conv);
    Long convId = conv.getConvId();

    log.info("[MAP_POST] Created conversation convId={} for creator={}", convId, creatorId);

    // 4. 添加 conversation_members
    // 4.1 创建者（群主）
    ConversationMember owner = new ConversationMember();
    owner.setConvId(convId);
    owner.setUserId(creatorId);
    owner.setConvRole(2); // 群主
    owner.setJoinedAt(now);
    conversationMemberMapper.insert(owner);

    // 4.2 其他可见成员
    for (Long memberId : memberIds) {
      if (memberId.equals(creatorId)) {
        continue; // 创建者已添加
      }

      ConversationMember member = new ConversationMember();
      member.setConvId(convId);
      member.setUserId(memberId);
      member.setConvRole(0); // 普通成员
      member.setJoinedAt(now);
      conversationMemberMapper.insert(member);
    }

    log.info("[MAP_POST] Added {} members to convId={}", memberIds.size(), convId);

    // 5. 创建 map_post
    MapPost mapPost = new MapPost();
    mapPost.setConvId(convId);
    mapPost.setCreatorId(creatorId);
    mapPost.setTitle(req.getTitle());
    mapPost.setDescription(req.getDescription());

    // 媒体URL转JSON
    if (req.getMediaUrls() != null && req.getMediaUrls().length > 0) {
      try {
        mapPost.setMediaJson(objectMapper.writeValueAsString(req.getMediaUrls()));
      } catch (JsonProcessingException e) {
        log.error("[MAP_POST] Failed to serialize mediaUrls", e);
        throw new IllegalArgumentException("Invalid mediaUrls format");
      }
    }

    // 位置信息
    mapPost.setLocLat(req.getLoc().getLat());
    mapPost.setLocLng(req.getLoc().getLng());
    mapPost.setLocName(req.getLoc().getName());
    mapPost.setGeohash(GeoHashUtil.encode(req.getLoc().getLat(), req.getLoc().getLng(), 7));

    // 初始化统计字段
    mapPost.setViewCount(0);
    mapPost.setLikeCount(0);
    mapPost.setCommentCount(0);
    mapPost.setStatus(1); // 已发布

    mapPost.setCreatedAt(now);
    mapPost.setUpdatedAt(now);
    mapPostMapper.insert(mapPost);

    log.info("[MAP_POST] Created map_post mapPostId={}, convId={}", mapPost.getMapPostId(), convId);

    // 6. 查询创建者信息（用于推送）
    User creator = userMapper.selectById(creatorId);
    String creatorUsername = (creator != null) ? creator.getUsername() : "Unknown";

    // 7. 构建返回结果
    long createdAtMs = now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

    return MapPostCreatedResult.builder()
        .mapPostId(mapPost.getMapPostId())
        .convId(convId)
        .creatorId(creatorId)
        .creatorUsername(creatorUsername)
        .title(req.getTitle())
        .description(req.getDescription())
        .mediaUrls(req.getMediaUrls())
        .locLat(req.getLoc().getLat())
        .locLng(req.getLoc().getLng())
        .locName(req.getLoc().getName())
        .createdAtMs(createdAtMs)
        .memberIds(memberIds)
        .build();
  }

  /**
   * 验证请求参数
   */
  private void validateRequest(WsCreateMapPost req) {
    if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
      throw new IllegalArgumentException("Title is required");
    }

    if (req.getLoc() == null || req.getLoc().getLat() == null || req.getLoc().getLng() == null) {
      throw new IllegalArgumentException("Location (lat, lng) is required");
    }

    double lat = req.getLoc().getLat();
    double lng = req.getLoc().getLng();
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      throw new IllegalArgumentException("Invalid latitude or longitude");
    }

    // 检查可见范围参数
    if (req.getAllFriends() == null) {
      throw new IllegalArgumentException("allFriends field is required");
    }

    if (!req.getAllFriends()) {
      // allFriends=false 时，必须提供 memberIds
      if (req.getMemberIds() == null || req.getMemberIds().length == 0) {
        throw new IllegalArgumentException("memberIds cannot be empty when allFriends is false");
      }
    }
  }

  /**
   * 根据可见范围确定成员列表
   */
  private List<Long> determineMemberIds(Long creatorId, WsCreateMapPost req) {
    if (req.getAllFriends()) {
      // 查询创建者的所有好友
      List<Long> friendIds = friendService.listFriends(creatorId).stream()
          .map(FriendView::getFriendId)
          .collect(Collectors.toList());

      // 包含创建者自己
      List<Long> result = new ArrayList<>(friendIds);
      if (!result.contains(creatorId)) {
        result.add(creatorId);
      }

      log.info("[MAP_POST] allFriends=true, found {} friends for creator={}", friendIds.size(), creatorId);
      return result;
    } else {
      // 使用指定的 memberIds
      List<Long> result = Arrays.stream(req.getMemberIds())
          .distinct()
          .collect(Collectors.toList());

      // 包含创建者自己
      if (!result.contains(creatorId)) {
        result.add(creatorId);
      }

      log.info("[MAP_POST] allFriends=false, specified {} members", result.size());
      return result;
    }
  }

}
