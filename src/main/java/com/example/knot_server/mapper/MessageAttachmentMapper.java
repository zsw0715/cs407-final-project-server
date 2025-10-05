package com.example.knot_server.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.entity.MessageAttachment;

/** 消息附件数据访问层 */
@Mapper
public interface MessageAttachmentMapper extends BaseMapper<MessageAttachment> {
    
}
