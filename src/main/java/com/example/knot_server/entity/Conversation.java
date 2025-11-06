package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("conversations")
public class Conversation {
    @TableId(value = "conv_id", type = IdType.AUTO)
    private Long convId;

    @TableField("conv_type")
    private Integer convType; // 1=single, 2=group

    private String title;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("last_msg_id")
    private Long lastMsgId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
