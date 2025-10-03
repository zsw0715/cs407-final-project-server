package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("conversation_members")
public class ConversationMember {
    @TableField("conv_id")
    private Long convId;

    @TableField("userid")
    private Long userId;

    @TableField("conv_role")
    private Integer convRole; // 0=member,1=admin,2=owner

    @TableField("joined_at")
    private LocalDateTime joinedAt;
}
