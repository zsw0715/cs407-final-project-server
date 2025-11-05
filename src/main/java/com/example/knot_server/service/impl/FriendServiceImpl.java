package com.example.knot_server.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.knot_server.entity.FriendRequests;
import com.example.knot_server.entity.Friends;
import com.example.knot_server.mapper.FriendRequestsMapper;
import com.example.knot_server.mapper.FriendsMapper;
import com.example.knot_server.service.ConversationService;
import com.example.knot_server.service.FriendService;
import com.example.knot_server.service.dto.FriendRequestView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
  private final FriendsMapper friendsMapper;
  private final FriendRequestsMapper friendRequestsMapper;
  private final ConversationService conversationService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public FriendRequestView sendFriendRequest(Long requesterId, Long receiverId, String message) {
    // 1. 参数校验
    if (requesterId == null || receiverId == null) {
      throw new IllegalArgumentException("Requester ID and Receiver ID cannot be null");
    }
    if (requesterId.equals(receiverId)) {
      throw new IllegalArgumentException("Cannot send friend request to yourself");
    }

    // 2. 检查是否已经是好友
    Long userMin = Math.min(requesterId, receiverId);
    Long userMax = Math.max(requesterId, receiverId);
    Friends existingFriend = friendsMapper.selectOne(
        new LambdaQueryWrapper<Friends>()
            .eq(Friends::getUserMinId, userMin)
            .eq(Friends::getUserMaxId, userMax)
            .last("LIMIT 1"));
    if (existingFriend != null) {
      throw new IllegalArgumentException("Already friends");
    }

        // 3. 检查是否已有申请记录（任何状态）
        FriendRequests existingRequest = friendRequestsMapper.selectOne(
                new LambdaQueryWrapper<FriendRequests>()
                        .eq(FriendRequests::getReqSenderId, requesterId)
                        .eq(FriendRequests::getReqReceiverId, receiverId)
                        .last("LIMIT 1"));
        
        FriendRequests request;
        
        if (existingRequest != null) {
            // 3.1 如果是待处理状态，不能重复发送
            if (existingRequest.getStatus() == 0) {
                throw new IllegalArgumentException("Friend request already sent");
            }
            
            // 3.2 如果是已拒绝（status=2），可以重新发送，更新现有记录
            if (existingRequest.getStatus() == 2) {
                existingRequest.setMessage(message);
                existingRequest.setStatus(0); // 重置为待处理
                existingRequest.setCreatedAt(LocalDateTime.now());
                existingRequest.setUpdatedAt(LocalDateTime.now());
                friendRequestsMapper.updateById(existingRequest);
                request = existingRequest;
            } 
            // 3.3 如果是已接受（status=1），说明已经是好友了（这种情况之前已经检查过了）
            else {
                throw new IllegalArgumentException("Friend request already accepted");
            }
        } else {
            // 4. 没有记录，创建新的好友申请
            request = new FriendRequests();
            request.setReqSenderId(requesterId);
            request.setReqReceiverId(receiverId);
            request.setMessage(message);
            request.setStatus(0); // 待处理
            friendRequestsMapper.insert(request);
        }

    // 5. 构建返回视图
    long createdAtMs = (request.getCreatedAt() != null)
        ? request.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        : System.currentTimeMillis();

    return FriendRequestView.builder()
        .requestId(request.getReqId())
        .requesterId(requesterId)
        .receiverId(receiverId)
        .message(message)
        .status(0)
        .createdAtMs(createdAtMs)
        .build();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public FriendRequestView acceptFriendRequest(Long requestId, Long currentUserId) {
    // 1. 查询申请 -- 如果申请不存在，抛出异常
    FriendRequests request = friendRequestsMapper.selectById(requestId);
    if (request == null) {
      throw new IllegalArgumentException("Friend request not found");
    }

    // 2. 验证权限（只有接收者可以接受）
    if (!request.getReqReceiverId().equals(currentUserId)) {
      throw new IllegalArgumentException("Only receiver can accept friend request");
    }

    // 3. 检查状态 - 如果申请已处理，抛出异常
    if (request.getStatus() != 0) {
      throw new IllegalArgumentException("Friend request already processed");
    }

    // 4. 更新申请状态
    request.setStatus(1); // 已接受
    request.setUpdatedAt(LocalDateTime.now());
    friendRequestsMapper.updateById(request);

    // 5. 创建好友关系
    Long userMin = Math.min(request.getReqSenderId(), request.getReqReceiverId());
    Long userMax = Math.max(request.getReqSenderId(), request.getReqReceiverId());

    Friends friends = new Friends();
    friends.setUserMinId(userMin);
    friends.setUserMaxId(userMax);
    friends.setCreatedAt(LocalDateTime.now());
    friendsMapper.insert(friends);

    // 6. 创建单聊会话
    Long convId = conversationService.getOrCreateSingleConv(
        request.getReqSenderId(),
        request.getReqReceiverId());

    // 7. 构建返回视图
    long createdAtMs = (request.getCreatedAt() != null)
        ? request.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        : System.currentTimeMillis();

    return FriendRequestView.builder()
        .requestId(requestId)
        .requesterId(request.getReqSenderId())
        .receiverId(request.getReqReceiverId())
        .message(request.getMessage())
        .status(1)
        .createdAtMs(createdAtMs)
        .convId(convId)
        .build();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public FriendRequestView rejectFriendRequest(Long requestId, Long currentUserId) {
    // 1. 查询申请
    FriendRequests request = friendRequestsMapper.selectById(requestId);
    if (request == null) {
      throw new IllegalArgumentException("Friend request not found");
    }

    // 2. 验证权限
    if (!request.getReqReceiverId().equals(currentUserId)) {
      throw new IllegalArgumentException("Only receiver can reject friend request");
    }

    // 3. 检查状态
    if (request.getStatus() != 0) {
      throw new IllegalArgumentException("Friend request already processed");
    }

    // 4. 更新申请状态
    request.setStatus(2); // 已拒绝
    request.setUpdatedAt(LocalDateTime.now());
    friendRequestsMapper.updateById(request);

    // 5. 构建返回视图
    long createdAtMs = (request.getCreatedAt() != null)
        ? request.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        : System.currentTimeMillis();

    return FriendRequestView.builder()
        .requestId(requestId)
        .requesterId(request.getReqSenderId())
        .receiverId(request.getReqReceiverId())
        .message(request.getMessage())
        .status(2)
        .createdAtMs(createdAtMs)
        .build();
  }

}
