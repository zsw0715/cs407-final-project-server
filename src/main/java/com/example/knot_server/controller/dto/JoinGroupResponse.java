package com.example.knot_server.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinGroupResponse {
    private Long conversationId;
    private Long userId;
}
