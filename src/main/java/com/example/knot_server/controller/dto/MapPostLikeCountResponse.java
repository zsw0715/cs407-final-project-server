package com.example.knot_server.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapPostLikeCountResponse {
    private Long mapPostId;
    private Integer likeCount;
}

