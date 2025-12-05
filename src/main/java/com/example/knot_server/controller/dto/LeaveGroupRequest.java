package com.example.knot_server.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveGroupRequest {
    private Long conversationId;
    private Long userId;
}
