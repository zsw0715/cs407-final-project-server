package com.example.knot_server.service;

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
}