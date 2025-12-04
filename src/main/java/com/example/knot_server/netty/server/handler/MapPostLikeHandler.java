package com.example.knot_server.netty.server.handler;

import org.springframework.stereotype.Component;

import com.example.knot_server.netty.server.model.WsMapPostLikeAction;
import com.example.knot_server.netty.server.model.WsMapPostLikeAck;
import com.example.knot_server.netty.server.model.WsMapPostLikeUpdate;
import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.LocalSessionRegistry;
import com.example.knot_server.service.MapPostService;
import com.example.knot_server.service.dto.MapPostLikeResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class MapPostLikeHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final ObjectMapper om;
  private final MapPostService mapPostService;
  private final LocalSessionRegistry registry;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    String text = msg.text();

    WsMapPostLikeAction req;
    try {
      req = om.readValue(text, WsMapPostLikeAction.class);
    } catch (Exception parseEx) {
      ctx.fireChannelRead(msg.retain());
      return;
    }

    if (req.getType() == null || !"MAP_POST_LIKE".equalsIgnoreCase(req.getType())) {
      ctx.fireChannelRead(msg.retain());
      return;
    }

    Long uid = ctx.channel().attr(ChannelAttrs.UID).get();
    if (uid == null) {
      ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"not authed\"}"));
      return;
    }

    try {
      if (req.getMapPostId() == null || req.getLiked() == null) {
        throw new IllegalArgumentException("mapPostId and liked are required");
      }

      MapPostLikeResult result = mapPostService.handleLikeAction(uid, req.getMapPostId(), req.getLiked());

      WsMapPostLikeAck ack = WsMapPostLikeAck.builder()
          .mapPostId(result.getMapPostId())
          .liked(result.getLiked())
          .likeCount(result.getLikeCount())
          .clientReqId(req.getClientReqId())
          .serverTime(System.currentTimeMillis())
          .build();
      ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(ack)));

      WsMapPostLikeUpdate update = WsMapPostLikeUpdate.builder()
          .mapPostId(result.getMapPostId())
          .userId(uid)
          .liked(result.getLiked())
          .likeCount(result.getLikeCount())
          .serverTime(System.currentTimeMillis())
          .build();
      String payload = om.writeValueAsString(update);

      if (result.getMemberIds() != null) {
        for (Long memberId : result.getMemberIds()) {
          if (memberId == null || memberId.equals(uid)) {
            continue;
          }

          try {
            Channel channel = registry.byUid(memberId);
            if (channel == null || !channel.isActive()) {
              continue;
            }
            channel.writeAndFlush(new TextWebSocketFrame(payload));
          } catch (Exception pushEx) {
            log.warn("[MAP_POST_LIKE] Failed to push update to uid={}: {}", memberId, pushEx.toString());
          }
        }
      }
    } catch (IllegalArgumentException bad) {
      var err = om.createObjectNode();
      err.put("type", "ERROR");
      err.put("msg", bad.getMessage() == null ? "bad request" : bad.getMessage());
      ctx.writeAndFlush(new TextWebSocketFrame(err.toString()));
    } catch (Exception e) {
      log.error("[MAP_POST_LIKE] Failed to process like action", e);
      ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"server error\"}"));
    }
  }
}
