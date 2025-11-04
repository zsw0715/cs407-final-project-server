package com.example.knot_server.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.entity.Friends;

/** 好友关系数据访问层 */
@Mapper
public interface FriendsMapper extends BaseMapper<Friends> {

}
