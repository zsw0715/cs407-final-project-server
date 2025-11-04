package com.example.knot_server.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.controller.dto.ConversationSummaryResponse;
import com.example.knot_server.entity.Conversation;
import com.example.knot_server.entity.Message;

/** 会话数据访问层 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    List<ConversationSummaryResponse> listUserConversations(Long userId);
    
    /**
     * 分页查询会话消息
     */
    List<Message> getConversationMessages(@Param("convId") Long convId, 
                                          @Param("size") int size, 
                                          @Param("offset") int offset);
    
    /**
     * 查询会话消息总数
     */
    Long countConversationMessages(@Param("convId") Long convId);
}
