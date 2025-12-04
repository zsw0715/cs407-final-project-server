package com.example.knot_server.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.knot_server.controller.dto.MapPostDetailResponse;
import com.example.knot_server.controller.dto.NearbyPostRequest;
import com.example.knot_server.controller.dto.NearbyPostRequestV2;
import com.example.knot_server.controller.dto.NearbyPostResponse;
import com.example.knot_server.entity.Conversation;
import com.example.knot_server.entity.ConversationMember;
import com.example.knot_server.entity.MapPost;
import com.example.knot_server.entity.MapPostLike;
import com.example.knot_server.entity.User;
import com.example.knot_server.mapper.ConversationMapper;
import com.example.knot_server.mapper.ConversationMemberMapper;
import com.example.knot_server.mapper.MapPostLikeMapper;
import com.example.knot_server.mapper.MapPostMapper;
import com.example.knot_server.mapper.UserMapper;
import com.example.knot_server.netty.server.model.WsCreateMapPost;
import com.example.knot_server.service.FriendService;
import com.example.knot_server.service.MapPostService;
import com.example.knot_server.service.dto.FriendView;
import com.example.knot_server.service.dto.MapPostCreatedResult;
import com.example.knot_server.service.dto.MapPostLikeResult;
import com.example.knot_server.util.GeoHashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapPostServiceImpl implements MapPostService {
  private final MapPostMapper mapPostMapper;
  private final MapPostLikeMapper mapPostLikeMapper;
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
    validateRequest_helper(req);

    // 2. 确定可见成员列表
    List<Long> memberIds = determineMemberIds_helper(creatorId, req);

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
    mapPost.setPostType(MapPost.PostType.valueOf(req.getPostType().toUpperCase()));

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
   * 验证请求参数 (helper function for createFromWebSocket)
   */
  private void validateRequest_helper(WsCreateMapPost req) {
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
   * (helper function for createFromWebSocket)
   */
  private List<Long> determineMemberIds_helper(Long creatorId, WsCreateMapPost req) {
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

  @Override
  public List<NearbyPostResponse> getNearbyPosts(NearbyPostRequest req, Long currentUserId) {
    log.info("[NEARBY] Query from user={}, zoom={}, center=({},{})",
        currentUserId, req.getZoomLevel(), req.getLat(), req.getLng());

    // 1. 参数校验
    if (req.getLat() == null || req.getLng() == null || req.getZoomLevel() == null) {
      throw new IllegalArgumentException("lat, lng, zoomLevel are required");
    }

    // 2. 根据zoom level确定查询策略
    QueryStrategy strategy = determineStrategy(req.getZoomLevel());

    // 3. 计算用户位置的GeoHash前缀
    String userGeohash = GeoHashUtil.encode(req.getLat(), req.getLng(), 7);
    String geohashPrefix = userGeohash.substring(0, strategy.prefixLength);

    log.info("[NEARBY] GeoHash strategy: prefix={}, prefixLength={}, maxResults={}",
        geohashPrefix, strategy.prefixLength, strategy.maxResults);

    // 4. 数据库查询（粗筛）
    LambdaQueryWrapper<MapPost> queryWrapper = new LambdaQueryWrapper<MapPost>()
        .likeRight(MapPost::getGeohash, geohashPrefix) // GeoHash前缀匹配
        .eq(MapPost::getStatus, 1) // 只查已发布的
        .ge(MapPost::getCreatedAt, getTimeRangeStart(req.getTimeRange())); // 时间过滤

    // 如果指定了postType（非ALL），添加过滤
    if (req.getPostType() != null && !"ALL".equalsIgnoreCase(req.getPostType())) {
      try {
        MapPost.PostType postType = MapPost.PostType.valueOf(req.getPostType().toUpperCase());
        queryWrapper.eq(MapPost::getPostType, postType);
      } catch (IllegalArgumentException e) {
        log.warn("[NEARBY] Invalid postType: {}", req.getPostType());
      }
    }

    // 按创建时间倒序，限制查询数量
    queryWrapper.orderByDesc(MapPost::getCreatedAt).last("LIMIT " + strategy.maxResults);

    List<MapPost> candidates = mapPostMapper.selectList(queryWrapper);
    log.info("[NEARBY] Found {} candidates from database", candidates.size());

    // 5. 精确过滤 + 构建Response
    List<NearbyPostResponse> results = candidates.stream()
        .filter(post -> {
          // 5.1 检查是否在视野边界内
          return isInBounds(post, req.getBounds());
        })
        .filter(post -> {
          // 5.2 权限检查：用户必须在conversation_members里
          long count = conversationMemberMapper.selectCount(
              new LambdaQueryWrapper<ConversationMember>()
                  .eq(ConversationMember::getConvId, post.getConvId())
                  .eq(ConversationMember::getUserId, currentUserId));
          return count > 0;
        })
        .map(post -> {
          // 5.3 计算精确距离
          double distance = GeoHashUtil.calculateDistance(
              req.getLat(), req.getLng(),
              post.getLocLat(), post.getLocLng());

          // 5.4 构建Response对象
          return buildResponse(post, distance, currentUserId);
        })
        .sorted(Comparator.comparing(NearbyPostResponse::getDistance)) // 按距离排序
        .collect(Collectors.toList());

    log.info("[NEARBY] After filtering, {} posts remain", results.size());

    // 6. 根据策略采样（如果需要）
    if (strategy.needsSampling && results.size() > strategy.targetCount) {
      results = samplePosts(results, strategy.targetCount);
      log.info("[NEARBY] Sampled to {} posts", results.size());
    }

    // 7. 限制最大返回数量
    int maxResults = Math.min(req.getMaxResults(), 100);
    if (results.size() > maxResults) {
      results = results.subList(0, maxResults);
    }

    log.info("[NEARBY] Returning {} posts to client", results.size());
    return results;
  }

  @Override
  public List<NearbyPostResponse> getNearbyPostsV2(NearbyPostRequestV2 req, Long currentUserId) {
    log.info("[NEARBY_V2] Simple query from user={}, center=({},{}), radius={}m",
        currentUserId, req.getLat(), req.getLng(), req.getRadius());

    // 1. 参数校验
    if (req.getLat() == null || req.getLng() == null) {
      throw new IllegalArgumentException("lat and lng are required");
    }

    // 2. 查询数据库：获取所有符合条件的posts（不用GeoHash）
    LambdaQueryWrapper<MapPost> queryWrapper = new LambdaQueryWrapper<MapPost>()
        .eq(MapPost::getStatus, 1)  // 已发布
        .ge(MapPost::getCreatedAt, getTimeRangeStart(req.getTimeRange()));  // 时间过滤

    // postType过滤
    if (req.getPostType() != null && !"ALL".equalsIgnoreCase(req.getPostType())) {
      try {
        MapPost.PostType postType = MapPost.PostType.valueOf(req.getPostType().toUpperCase());
        queryWrapper.eq(MapPost::getPostType, postType);
      } catch (IllegalArgumentException e) {
        log.warn("[NEARBY_V2] Invalid postType: {}", req.getPostType());
      }
    }

    // 限制查询数量，避免全表扫描
    queryWrapper.last("LIMIT 1000");

    List<MapPost> allPosts = mapPostMapper.selectList(queryWrapper);
    log.info("[NEARBY_V2] Found {} posts from database", allPosts.size());

    // 3. 计算距离 + 权限过滤 + 半径过滤
    List<NearbyPostResponse> results = allPosts.stream()
        .filter(post -> {
          // 权限检查
          long count = conversationMemberMapper.selectCount(
              new LambdaQueryWrapper<ConversationMember>()
                  .eq(ConversationMember::getConvId, post.getConvId())
                  .eq(ConversationMember::getUserId, currentUserId));
          return count > 0;
        })
        .map(post -> {
          // 计算距离
          double distance = GeoHashUtil.calculateDistance(
              req.getLat(), req.getLng(),
              post.getLocLat(), post.getLocLng());
          return new PostWithDistance(post, distance);
        })
        .filter(pwd -> pwd.distance <= req.getRadius())  // 半径过滤
        .sorted(Comparator.comparing(pwd -> pwd.distance))  // 按距离排序
        .limit(req.getMaxResults())
        .map(pwd -> buildResponse(pwd.post, pwd.distance, currentUserId))
        .collect(Collectors.toList());

    log.info("[NEARBY_V2] Returning {} posts within {}m radius", results.size(), req.getRadius());
    return results;
  }

  // ========== 辅助方法（Helper Methods） ==========

  /**
   * 辅助类：Post + Distance（用于V2）
   */
  private static class PostWithDistance {
    MapPost post;
    double distance;

    PostWithDistance(MapPost post, double distance) {
      this.post = post;
      this.distance = distance;
    }
  }

  /**
   * 查询策略内部类
   */
  @Data
  @AllArgsConstructor
  private static class QueryStrategy {
    int prefixLength; // GeoHash前缀长度
    int maxResults; // 查询上限
    int targetCount; // 期望返回数量
    boolean needsSampling; // 是否需要采样
  }

  /**
   * 根据Zoom Level确定查询策略
   * MapBox Zoom Level参考：
   * - 0-3: 世界级别
   * - 4-6: 国家级别
   * - 7-10: 城市级别
   * - 11-14: 街区级别
   * - 15-18: 街道级别
   * - 19-22: 建筑级别
   */
  private QueryStrategy determineStrategy(Integer zoomLevel) {
    if (zoomLevel == null) {
      zoomLevel = 12; // 默认街区级别
    }

    if (zoomLevel >= 16) {
      // 街道级别：返回所有，不采样
      return new QueryStrategy(7, 100, 100, false);
    } else if (zoomLevel >= 13) {
      // 街区级别：适度采样
      return new QueryStrategy(6, 200, 50, true);
    } else if (zoomLevel >= 10) {
      // 城市级别：大量采样
      return new QueryStrategy(5, 500, 30, true);
    } else {
      // 国家/世界级别：只返回热门posts
      return new QueryStrategy(4, 1000, 20, true);
    }
  }

  /**
   * 根据时间范围计算起始时间
   */
  private LocalDateTime getTimeRangeStart(String timeRange) {
    LocalDateTime now = LocalDateTime.now();
    switch (timeRange) {
      case "1D":
        return now.minusDays(1);
      case "7D":
        return now.minusDays(7);
      case "30D":
        return now.minusDays(30);
      default:
        return now.minusDays(7); // 默认7天
    }
  }

  /**
   * 检查post是否在视野边界内
   */
  private boolean isInBounds(MapPost post, NearbyPostRequest.BoundingBox bounds) {
    if (bounds == null) {
      return true; // 没有提供bounds，不过滤
    }

    double lat = post.getLocLat();
    double lng = post.getLocLng();

    return lat <= bounds.getNorthEastLat()
        && lat >= bounds.getSouthWestLat()
        && lng <= bounds.getNorthEastLng()
        && lng >= bounds.getSouthWestLng();
  }

  /**
   * 智能采样：优先保留热门/最新的posts
   */
  private List<NearbyPostResponse> samplePosts(List<NearbyPostResponse> posts, int targetCount) {
    if (posts.size() <= targetCount) {
      return posts;
    }

    // 按综合评分排序：点赞数 * 2 + 评论数 * 3 - 时间衰减
    return posts.stream()
        .sorted((a, b) -> {
          long scoreA = a.getLikeCount() * 2L + a.getCommentCount() * 3L;
          long scoreB = b.getLikeCount() * 2L + b.getCommentCount() * 3L;

          // 时间衰减：越新的分数越高（每小时减1分）
          long ageA = System.currentTimeMillis() - a.getCreatedAtMs();
          long ageB = System.currentTimeMillis() - b.getCreatedAtMs();
          scoreA -= ageA / (1000 * 60 * 60);
          scoreB -= ageB / (1000 * 60 * 60);

          return Long.compare(scoreB, scoreA); // 降序
        })
        .limit(targetCount)
        .collect(Collectors.toList());
  }

  /**
   * 构建Response对象
   */
  private NearbyPostResponse buildResponse(MapPost post, double distance, Long currentUserId) {
    // 查询创建者信息
    User creator = userMapper.selectById(post.getCreatorId());

    // 解析mediaUrls
    String[] mediaUrls = null;
    if (post.getMediaJson() != null && !post.getMediaJson().isEmpty()) {
      try {
        mediaUrls = objectMapper.readValue(post.getMediaJson(), String[].class);
      } catch (Exception e) {
        log.warn("[NEARBY] Failed to parse mediaJson for mapPostId={}", post.getMapPostId());
      }
    }

    // 计算创建时间戳
    long createdAtMs = post.getCreatedAt() != null
        ? post.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        : System.currentTimeMillis();

    return NearbyPostResponse.builder()
        .mapPostId(post.getMapPostId())
        .convId(post.getConvId())
        .title(post.getTitle())
        .description(post.getDescription())
        .mediaUrls(mediaUrls)
        .locLat(post.getLocLat())
        .locLng(post.getLocLng())
        .locName(post.getLocName())
        .distance(distance)
        .creatorId(post.getCreatorId())
        .creatorUsername(creator != null ? creator.getUsername() : "Unknown")
        .creatorAvatar(creator != null ? creator.getAvatarUrl() : null)
        .viewCount(post.getViewCount())
        .likeCount(post.getLikeCount())
        .commentCount(post.getCommentCount())
        .postType(post.getPostType().name())
        .createdAtMs(createdAtMs)
        .build();
  }

  /**
   * 根据 mapPostId 获取帖子详情
   * @param mapPostId 帖子ID
   * @param currentUserId 当前用户ID
   * @return 帖子详情
   */
  @Override
  public MapPostDetailResponse getMapPostDetailByMapPostId(Long mapPostId, Long currentUserId) {
    log.info("[MAP_POST_DETAIL] User {} requesting detail for mapPostId={}", currentUserId, mapPostId);

    // 1. 查询map_posts表
    MapPost post = mapPostMapper.selectById(mapPostId);
    if (post == null) {
      throw new IllegalArgumentException("Map post not found: " + mapPostId);
    }

    // 2. 检查权限：用户是否在conversation_members中
    long memberCount = conversationMemberMapper.selectCount(
        new LambdaQueryWrapper<ConversationMember>()
            .eq(ConversationMember::getConvId, post.getConvId())
            .eq(ConversationMember::getUserId, currentUserId));

    if (memberCount == 0) {
      throw new RuntimeException("无权访问该帖子");
    }

    // 3. 查询作者信息
    User creator = userMapper.selectById(post.getCreatorId());
    if (creator == null) {
      throw new RuntimeException("作者信息不存在");
    }

    // 4. 检查当前用户是否点赞了这个帖子
    long likeCount = mapPostLikeMapper.selectCount(
        new LambdaQueryWrapper<MapPostLike>()
            .eq(MapPostLike::getMapPostId, mapPostId)
            .eq(MapPostLike::getUserId, currentUserId));
    boolean isLikedByCurrentUser = likeCount > 0;

    // 5. 增加浏览数（排除作者自己）
    if (!currentUserId.equals(post.getCreatorId())) {
      post.setViewCount(post.getViewCount() + 1);
      mapPostMapper.updateById(post);
      log.info("[MAP_POST_DETAIL] View count incremented for mapPostId={}", mapPostId);
    }

    // 6. 解析mediaUrls（从JSON字符串转为List）
    List<String> mediaUrls = null;
    if (post.getMediaJson() != null && !post.getMediaJson().isEmpty()) {
      try {
        // 假设mediaJson格式为 ["url1","url2"]
        mediaUrls = objectMapper.readValue(post.getMediaJson(), 
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
      } catch (Exception e) {
        log.warn("[MAP_POST_DETAIL] Failed to parse mediaJson for mapPostId={}", mapPostId, e);
        mediaUrls = List.of(); // 返回空列表
      }
    }

    // 7. 组装返回对象
    MapPostDetailResponse response = MapPostDetailResponse.builder()
        .mapPostId(post.getMapPostId())
        .convId(post.getConvId())
        .title(post.getTitle())
        .description(post.getDescription())
        .mediaUrls(mediaUrls)
        .creatorId(post.getCreatorId())
        .creatorUsername(creator.getUsername())
        .creatorAvatar(creator.getAvatarUrl())
        .locLat(post.getLocLat())
        .locLng(post.getLocLng())
        .locName(post.getLocName())
        .viewCount(post.getViewCount() + (currentUserId.equals(post.getCreatorId()) ? 0 : 1)) // 显示增加后的值
        .likeCount(post.getLikeCount())
        .commentCount(post.getCommentCount())
        .createdAtMs(post.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        .postType(post.getPostType().name())
        .isLikedByCurrentUser(isLikedByCurrentUser)
        .build();

    log.info("[MAP_POST_DETAIL] Returning detail for mapPostId={}", mapPostId);
    return response;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public MapPostLikeResult handleLikeAction(Long userId, Long mapPostId, boolean like) {
    MapPost mapPost = mapPostMapper.selectById(mapPostId);
    if (mapPost == null || mapPost.getStatus() == null || mapPost.getStatus() != 1) {
      throw new IllegalArgumentException("Map post not found");
    }

    User liker = userMapper.selectById(userId);
    String likerNickname = null;
    if (liker != null) {
      if (StringUtils.hasText(liker.getNickname())) {
        likerNickname = liker.getNickname();
      } else if (StringUtils.hasText(liker.getUsername())) {
        likerNickname = liker.getUsername();
      }
    }
    if (!StringUtils.hasText(likerNickname)) {
      likerNickname = "用户" + userId;
    }

    long memberCount = conversationMemberMapper.selectCount(
        new LambdaQueryWrapper<ConversationMember>()
            .eq(ConversationMember::getConvId, mapPost.getConvId())
            .eq(ConversationMember::getUserId, userId));
    if (memberCount == 0) {
      throw new IllegalArgumentException("无权操作该帖子");
    }

    boolean alreadyLiked = mapPostLikeMapper.selectCount(userLikeQuery(mapPostId, userId)) > 0;

    if (like && !alreadyLiked) {
      MapPostLike record = new MapPostLike();
      record.setMapPostId(mapPostId);
      record.setUserId(userId);
      record.setCreatedAt(LocalDateTime.now());
      mapPostLikeMapper.insert(record);
    } else if (!like && alreadyLiked) {
      mapPostLikeMapper.delete(userLikeQuery(mapPostId, userId));
    }

    int likeCount = Math.toIntExact(mapPostLikeMapper.selectCount(
        new LambdaQueryWrapper<MapPostLike>()
            .eq(MapPostLike::getMapPostId, mapPostId)));

    mapPost.setLikeCount(likeCount);
    mapPost.setUpdatedAt(LocalDateTime.now());
    mapPostMapper.updateById(mapPost);

    List<Long> memberIds = conversationMemberMapper.selectList(
        new LambdaQueryWrapper<ConversationMember>()
            .eq(ConversationMember::getConvId, mapPost.getConvId()))
        .stream()
        .map(ConversationMember::getUserId)
        .collect(Collectors.toList());

    boolean finalLiked = mapPostLikeMapper.selectCount(userLikeQuery(mapPostId, userId)) > 0;

    return MapPostLikeResult.builder()
        .mapPostId(mapPostId)
        .convId(mapPost.getConvId())
        .likeCount(likeCount)
        .liked(finalLiked)
        .memberIds(memberIds)
        .likerNickname(likerNickname)
        .build();
  }

  @Override
  public int getMapPostLikeCount(Long mapPostId) {
    MapPost mapPost = mapPostMapper.selectById(mapPostId);
    if (mapPost == null || mapPost.getStatus() == null || mapPost.getStatus() != 1) {
      throw new IllegalArgumentException("Map post not found");
    }

    return Math.toIntExact(mapPostLikeMapper.selectCount(
        new LambdaQueryWrapper<MapPostLike>()
            .eq(MapPostLike::getMapPostId, mapPostId)));
  }

  private LambdaQueryWrapper<MapPostLike> userLikeQuery(Long mapPostId, Long userId) {
    return new LambdaQueryWrapper<MapPostLike>()
        .eq(MapPostLike::getMapPostId, mapPostId)
        .eq(MapPostLike::getUserId, userId);
  }

}
