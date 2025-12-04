package com.example.knot_server.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MapPostLikeResult {
    private Long mapPostId;
    private Long convId;
    private Integer likeCount;
    private Boolean liked;
    private List<Long> memberIds;
}
