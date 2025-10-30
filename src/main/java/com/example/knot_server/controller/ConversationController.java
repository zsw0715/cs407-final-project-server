package com.example.knot_server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.knot_server.controller.dto.ApiResponse;
import com.example.knot_server.controller.dto.ConversationSummaryResponse;
import com.example.knot_server.controller.dto.IdResponse;
import com.example.knot_server.service.ConversationService;
import com.example.knot_server.util.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    /**
     * 获取或创建单聊会话
     */
    @PostMapping("/getOrCreateSingle")
    public ResponseEntity<ApiResponse<IdResponse>> getOrCreateSingle(@RequestParam Long otherUserId, Authentication auth) {
        // 从JWT认证信息中获取当前用户ID
        JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
        Long currentUserId = principal.uid();

        Long convId = conversationService.getOrCreateSingleConv(currentUserId, otherUserId);
        if (convId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("无法创建或获取会话"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("会话获取成功", new IdResponse(convId)));
    }

    /**
     * 获取或创建群聊会话
     */
    @PostMapping("/createGroup")
    public ResponseEntity<ApiResponse<IdResponse>> createGroup(@RequestParam String groupName,
            @RequestParam(required = false) Long[] memberIds, Authentication auth) {
        // 从JWT认证信息中获取当前用户ID
        JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
        Long currentUserId = principal.uid();
        Long convId = conversationService.createGroupConv(groupName, memberIds, currentUserId);
        if (convId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("无法创建群会话"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("组会话创建成功", new IdResponse(convId)));
    }

    /**
     * 获取用户的所有会话 base on userId  (获得 only group conversation and single conversation)
     */
    @GetMapping("list")
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> listUserConversations(Authentication auth) {
        JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
        Long currentUserId = principal.uid();

        List<ConversationSummaryResponse> conversationsList = conversationService.listUserConversations(currentUserId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("会话列表获取成功", conversationsList));
    }

    /**
     * 分页获取会话中 messages
     */
    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Object>> getConversationMessages(@RequestParam Long conversationId,
            @RequestParam int page, @RequestParam int size, Authentication auth) {
        JwtAuthFilter.SimplePrincipal principal = (JwtAuthFilter.SimplePrincipal) auth.getPrincipal();
        Long currentUserId = principal.uid();

        return null;
    }
}
