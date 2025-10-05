package com.example.knot_server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.knot_server.controller.dto.ApiResponse;
import com.example.knot_server.service.ConversationService;

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
    public ResponseEntity<ApiResponse<?>> getOrCreateSingle(@RequestParam Long uidA, @RequestParam Long uidB) {
        Long convId = conversationService.getOrCreateSingleConv(uidA, uidB);
        if (convId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("无法创建或获取会话"));
        } 
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("会话获取成功", convId));
    }
}
