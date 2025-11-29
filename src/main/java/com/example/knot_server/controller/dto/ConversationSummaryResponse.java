package com.example.knot_server.controller.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConversationSummaryResponse {

    private Long convId;
    private Integer convType;
    private String title;
    private Long creatorId;

    // --- 单聊对方 ---
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;

    // --- 自己（新增） ---
    private Long selfUserId;
    private String selfUserName;
    private String selfUserAvatar;

    // --- 群聊 ---
    private String groupAvatar;
    private Integer memberCount;

    // --- 公共 ---
    private String lastMsgPreview;
    private Integer lastMsgType;
    private LocalDateTime lastMsgTime;
    private LocalDateTime updatedAt;
}