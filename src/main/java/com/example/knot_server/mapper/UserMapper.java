package com.example.knot_server.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.entity.User;

/** 用户数据访问层 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
        
}
