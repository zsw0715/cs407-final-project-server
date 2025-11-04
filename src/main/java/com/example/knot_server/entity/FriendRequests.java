package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("friend_requests")
public class FriendRequests {
  @TableId(value = "req_id", type = IdType.AUTO)
  private Long reqId;

  @TableField("req_sender_id")
  private Long reqSenderId;

  @TableField("req_receiver_id")
  private Long reqReceiverId;

  @TableField("message")
  private String message;

  @TableField("status")
  private Integer status;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;
}
