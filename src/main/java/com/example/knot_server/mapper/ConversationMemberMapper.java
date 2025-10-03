package com.example.knot_server.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.entity.ConversationMember;

@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {

}
