package com.example.knot_server.service.impl;

import org.springframework.stereotype.Service;

import com.example.knot_server.mapper.FriendRequestsMapper;
import com.example.knot_server.mapper.FriendsMapper;
import com.example.knot_server.service.FriendService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
  private final FriendsMapper friendsMapper;
  private final FriendRequestsMapper friendRequestsMapper;
  
}
