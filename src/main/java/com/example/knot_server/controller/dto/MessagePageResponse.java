package com.example.knot_server.controller.dto;

import java.util.List;

import com.example.knot_server.entity.Message;

import lombok.Data;

/**
 * 分页获取会话中 messages
 */
@Data
public class MessagePageResponse {
  private Long convId;
  private Integer page;
  private Integer size;
  private Long total; // 总记录数
  private Integer totalPages; // 总页数
  private List<Message> messageList;
}
