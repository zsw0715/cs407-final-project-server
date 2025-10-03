package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("single_conv_index")
public class SingleConvIndex {
    @TableField("user_min_id")
    private Long userMinId;

    @TableField("user_max_id")
    private Long userMaxId;

    @TableField("conv_id")
    private Long convId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
