package com.example.knot_server.service;

import java.util.List;

import com.example.knot_server.controller.dto.NearbyPostRequest;
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
     * 获取附近的地图帖子
     * @param req 请求参数
     * @param currentUserId 当前用户ID
     * @return 地图帖子列表
     */
    List<NearbyPostResponse> getNearbyPosts(NearbyPostRequest req, Long currentUserId);
}