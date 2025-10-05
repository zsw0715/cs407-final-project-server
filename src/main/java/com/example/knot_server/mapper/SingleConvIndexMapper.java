package com.example.knot_server.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.entity.SingleConvIndex;

/** 单聊会话索引数据访问层 */
@Mapper
public interface SingleConvIndexMapper extends BaseMapper<SingleConvIndex> {
    
}
