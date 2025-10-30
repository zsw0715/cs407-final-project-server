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
    private String avatarUrl;           // 来自 user 表
    private String lastMsgPreview;      // 来自 messages
    private Integer lastMsgType;        // messages.msg_type
    private LocalDateTime lastMsgTime;  // messages.created_at
    private LocalDateTime updatedAt;
}
