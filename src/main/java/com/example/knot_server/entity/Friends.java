package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("friends")
public class Friends {
  @TableField("user_min_id")
  private Long userMinId;

  @TableField("user_max_id")
  private Long userMaxId;

  @TableField("created_at")
  private LocalDateTime createdAt;
}
