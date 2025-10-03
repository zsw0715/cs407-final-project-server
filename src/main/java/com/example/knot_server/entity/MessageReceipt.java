package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("message_receipts")
public class MessageReceipt {
    
    @TableField("msg_id")
    private Long msgId;

    @TableField("userid")
    private Long userId;

    @TableField("delivered_at")
    private LocalDateTime deliveredAt;

}
