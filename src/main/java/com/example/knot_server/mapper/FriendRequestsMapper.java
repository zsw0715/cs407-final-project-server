package com.example.knot_server.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.knot_server.entity.FriendRequests;

/** 好友申请数据访问层 */
@Mapper
public interface FriendRequestsMapper extends BaseMapper<FriendRequests> {
  
}
