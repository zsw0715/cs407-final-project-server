package com.example.knot_server.netty.server.handler;

import org.springframework.stereotype.Component;

import com.example.knot_server.entity.User;
import com.example.knot_server.mapper.UserMapper;
import com.example.knot_server.netty.server.model.WsFriendAdded;
import com.example.knot_server.netty.server.model.WsFriendRequestAck;
import com.example.knot_server.netty.server.model.WsFriendRequestNew;
import com.example.knot_server.netty.server.model.WsFriendRequestResponse;
import com.example.knot_server.netty.server.model.WsFriendRequestSend;
import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.LocalSessionRegistry;
import com.example.knot_server.service.FriendService;
import com.example.knot_server.service.dto.FriendRequestView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 好友请求 WebSocket Handler
 * 处理好友申请的发送、接受、拒绝
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class FriendRequestHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final ObjectMapper om;
  private final FriendService friendService;
  private final UserMapper userMapper;
  private final LocalSessionRegistry registry;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    String text = msg.text();

    // 解析消息类型
    JsonNode node;
    try {
      node = om.readTree(text);
    } catch (Exception parseEx) {
      ctx.fireChannelRead(msg.retain());
      return;
    }

    // 获取消息类型 -- 如果类型为空，则返回错误
    String type = node.has("type") ? node.get("type").asText() : null;
    if (type == null) {
      ctx.fireChannelRead(msg.retain());
      return;
    }

    // 检查是否是好友相关消息
    if (!type.startsWith("FRIEND_REQUEST_")) {
      ctx.fireChannelRead(msg.retain());
      return;
    }

    // 必须已认证
    Long uid = ctx.channel().attr(ChannelAttrs.UID).get();
    if (uid == null) {
      ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"not authed\"}"));
      return;
    }

    // 处理好友请求, 根据类型调用不同的处理方法
    try {
      switch (type) {
        case "FRIEND_REQUEST_SEND":
          handleSendRequest(ctx, text, uid);
          break;
        case "FRIEND_REQUEST_ACCEPT":
          handleAcceptRequest(ctx, text, uid);
          break;
        case "FRIEND_REQUEST_REJECT":
          handleRejectRequest(ctx, text, uid);
          break;
        default:
          ctx.fireChannelRead(msg.retain());
      }
    } catch (IllegalArgumentException bad) {
      var err = om.createObjectNode();
      err.put("type", "ERROR");
      err.put("msg", bad.getMessage() == null ? "bad request" : bad.getMessage());
      ctx.writeAndFlush(new TextWebSocketFrame(err.toString()));
    } catch (Exception e) {
      log.error("Friend request handling failed", e);
      ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"server error\"}"));
    }

  }

  /**
   * 处理发送好友申请
   */
  private void handleSendRequest(ChannelHandlerContext ctx, String text, Long uid) throws Exception {
    // 1. 解析请求
    WsFriendRequestSend req = om.readValue(text, WsFriendRequestSend.class);
    if (req.getReceiverId() == null) {
      throw new IllegalArgumentException("Receiver ID is required");
    }

    // 2. 调用 Service 处理 -- 存入数据库
    FriendRequestView result = friendService.sendFriendRequest(uid, req.getReceiverId(), req.getMessage());

    // 3. 回复 ACK 给发送者
    WsFriendRequestAck ack = WsFriendRequestAck.builder()
        .type("FRIEND_REQUEST_ACK")
        .requestId(result.getRequestId())
        .status("sent")
        .timestamp(System.currentTimeMillis())
        .build();
    ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(ack)));

    // 4. 查询发送者信息
    User requester = userMapper.selectById(uid);
    if (requester == null) {
      log.warn("Requester user {} not found", uid);
      return;
    }

    // 5. 推送给接收者（如果在线） -- FRIEND_REQUEST_NEW
    Channel receiverChannel = registry.byUid(req.getReceiverId());
    if (receiverChannel != null && receiverChannel.isActive()) {
      WsFriendRequestNew pushToReceiver = WsFriendRequestNew.builder()
          .type("FRIEND_REQUEST_NEW")
          .requestId(result.getRequestId())
          .fromUser(WsFriendRequestNew.UserInfo.builder()
              .userId(uid)
              .username(requester.getUsername())
              .avatarUrl(requester.getAvatarUrl())
              .build())
          .message(req.getMessage())
          .timestamp(result.getCreatedAtMs())
          .build();
      receiverChannel.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(pushToReceiver)));
      log.info("Pushed friend request {} to receiver {}", result.getRequestId(), req.getReceiverId());
    }
  }

  /**
   * 处理接受好友申请
   */
  private void handleAcceptRequest(ChannelHandlerContext ctx, String text, Long uid) throws Exception {
    // 1. 解析请求
    WsFriendRequestResponse req = om.readValue(text, WsFriendRequestResponse.class);
    if (req.getRequestId() == null) {
      throw new IllegalArgumentException("Request ID is required");
    }

    // 2. 调用 Service 处理（会创建好友关系和会话）
    FriendRequestView result = friendService.acceptFriendRequest(req.getRequestId(), uid);

    // 3. 回复 ACK 给接受者
    WsFriendRequestAck ack = WsFriendRequestAck.builder()
        .type("FRIEND_REQUEST_ACK")
        .requestId(result.getRequestId())
        .status("accepted")
        .convId(result.getConvId())
        .timestamp(System.currentTimeMillis())
        .build();
    ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(ack)));

    // 4. 查询接受者信息
    User accepter = userMapper.selectById(uid);
    if (accepter == null) {
      log.warn("Accepter user {} not found", uid);
      return;
    }

    // 5. 推送给申请发起者（如果在线）
    Channel requesterChannel = registry.byUid(result.getRequesterId());
    if (requesterChannel != null && requesterChannel.isActive()) {
      WsFriendAdded pushToRequester = WsFriendAdded.builder()
          .type("FRIEND_ADDED")
          .requestId(result.getRequestId())
          .friend(WsFriendAdded.FriendInfo.builder()
              .userId(uid)
              .username(accepter.getUsername())
              .avatarUrl(accepter.getAvatarUrl())
              .build())
          .convId(result.getConvId())
          .timestamp(System.currentTimeMillis())
          .build();
      requesterChannel.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(pushToRequester)));
      log.info("Pushed friend added notification to requester {}", result.getRequesterId());
    }
  }

  /**
   * 处理拒绝好友申请
   */
  private void handleRejectRequest(ChannelHandlerContext ctx, String text, Long uid) throws Exception {
    // 1. 解析请求
    WsFriendRequestResponse req = om.readValue(text, WsFriendRequestResponse.class);
    if (req.getRequestId() == null) {
      throw new IllegalArgumentException("Request ID is required");
    }

    // 2. 调用 Service 处理
    FriendRequestView result = friendService.rejectFriendRequest(req.getRequestId(), uid);

    // 3. 回复 ACK 给拒绝者
    WsFriendRequestAck ack = WsFriendRequestAck.builder()
        .type("FRIEND_REQUEST_ACK")
        .requestId(result.getRequestId())
        .status("rejected")
        .timestamp(System.currentTimeMillis())
        .build();
    ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(ack)));

    // 4. 可选：推送给申请发起者（告知被拒绝）
    // 根据产品需求决定是否通知发起者
    log.info("Friend request {} rejected by {}", result.getRequestId(), uid);
  }

}
