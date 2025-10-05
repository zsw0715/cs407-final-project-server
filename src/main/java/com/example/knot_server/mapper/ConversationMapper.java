package com.example.knot_server.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.entity.Conversation;

/** 会话数据访问层 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    
}
