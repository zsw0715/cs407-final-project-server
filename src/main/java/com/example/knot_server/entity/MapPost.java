package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("map_posts")
public class MapPost {
    
    @TableId(value = "map_post_id", type = IdType.AUTO)
    private Long mapPostId;

    @TableField("conv_id")
    private Long convId;

    @TableField("creator_id")
    private Long creatorId;

    private String title;

    private String description;

    @TableField("media_json")
    private String mediaJson;  // JSON字符串，例: ["url1","url2"]

    @TableField("loc_lat")
    private Double locLat;

    @TableField("loc_lng")
    private Double locLng;

    @TableField("loc_name")
    private String locName;

    private String geohash;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("comment_count")
    private Integer commentCount;

    private Integer status;  // 0=已删除, 1=已发布

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
