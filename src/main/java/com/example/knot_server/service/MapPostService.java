package com.example.knot_server.service;

import java.util.List;

import com.example.knot_server.controller.dto.MapPostDetailResponse;
import com.example.knot_server.controller.dto.NearbyPostRequest;
import com.example.knot_server.controller.dto.NearbyPostRequestV2;
import com.example.knot_server.controller.dto.NearbyPostResponse;
import com.example.knot_server.netty.server.model.WsCreateMapPost;
import com.example.knot_server.service.dto.MapPostCreatedResult;

/**
 * Map Post服务接口
 */
public interface MapPostService {
    
    /**
     * 从WebSocket创建Map Post
     * @param creatorId 创建者用户ID
     * @param req WebSocket请求
     * @return 创建结果（包含mapPostId、convId、成员列表）
     */
    MapPostCreatedResult createFromWebSocket(Long creatorId, WsCreateMapPost req);

    /**
     * 获取附近的地图帖子（V1: GeoHash-based）
     * @param req 请求参数
     * @param currentUserId 当前用户ID
     * @return 地图帖子列表
     */
    List<NearbyPostResponse> getNearbyPosts(NearbyPostRequest req, Long currentUserId);

    /**
     * 获取附近的地图帖子（V2: 简化版，radius-based）
     * @param req 请求参数
     * @param currentUserId 当前用户ID
     * @return 地图帖子列表
     */
    List<NearbyPostResponse> getNearbyPostsV2(NearbyPostRequestV2 req, Long currentUserId);

    /**
     * 根据 mapPostId 获取帖子详情
     * @param mapPostId 帖子ID
     * @param currentUserId 当前用户ID（用于权限检查和判断是否点赞）
     * @return 帖子详情
     */
    MapPostDetailResponse getMapPostDetailByMapPostId(Long mapPostId, Long currentUserId);
}