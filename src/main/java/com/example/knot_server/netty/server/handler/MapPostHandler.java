package com.example.knot_server.netty.server.handler;

import org.springframework.stereotype.Component;

import com.example.knot_server.netty.server.model.WsCreateMapPost;
import com.example.knot_server.netty.server.model.WsMapPostAck;
import com.example.knot_server.netty.server.model.WsMapPostNew;
import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.LocalSessionRegistry;
import com.example.knot_server.service.MapPostService;
import com.example.knot_server.service.dto.MapPostCreatedResult;
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
public class MapPostHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final ObjectMapper om;
  private final MapPostService mapPostService;
  private final LocalSessionRegistry registry;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    String text = msg.text();

    WsCreateMapPost req;
    try {
      req = om.readValue(text, WsCreateMapPost.class);
    } catch (Exception parseEx) {
      // 不是本handler关心的消息，传递给下一个handler
      ctx.fireChannelRead(msg.retain());
      return;
    }

    if (req.getType() == null || !"MAP_POST_CREATE".equalsIgnoreCase(req.getType())) {
      ctx.fireChannelRead(msg.retain());
      return;
    }

    // 必须已认证
    Long uid = ctx.channel().attr(ChannelAttrs.UID).get();
    if (uid == null) {
      ctx.writeAndFlush(new TextWebSocketFrame(
          "{\"type\":\"ERROR\",\"msg\":\"not authenticated\"}"));
      return;
    }

    try {
      log.info("[MAP_POST] Received MAP_POST_CREATE from uid={}", uid);

      // 1. 调用Service创建Map Post
      MapPostCreatedResult result = mapPostService.createFromWebSocket(uid, req);

      // 2. 返回ACK给创建者
      WsMapPostAck ack = WsMapPostAck.builder()
          .type("MAP_POST_ACK")
          .clientReqId(req.getClientReqId())
          .mapPostId(result.getMapPostId())
          .convId(result.getConvId())
          .serverTime(System.currentTimeMillis())
          .build();

      ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(ack)));
      log.info("[MAP_POST] Sent ACK to creator uid={}, mapPostId={}", uid, result.getMapPostId());

      // 3. 推送 MAP_POST_NEW 给所有可见成员
      WsMapPostNew pushMsg = WsMapPostNew.builder()
          .type("MAP_POST_NEW")
          .mapPostId(result.getMapPostId())
          .convId(result.getConvId())
          .creatorId(result.getCreatorId())
          .creatorUsername(result.getCreatorUsername())
          .title(result.getTitle())
          .description(result.getDescription())
          .mediaUrls(result.getMediaUrls())
          .loc(WsMapPostNew.Loc.builder()
              .lat(result.getLocLat())
              .lng(result.getLocLng())
              .name(result.getLocName())
              .build())
          .createdAtMs(result.getCreatedAtMs())
          .build();
      String payload = om.writeValueAsString(pushMsg);

      // 广播给所有可见成员（包括创建者自己，客户端可以根据creatorId判断）
      for (Long memberId : result.getMemberIds()) {
        if (memberId == null)
          continue;

        try {
          Channel ch = registry.byUid(memberId);
          if (ch == null || !ch.isActive()) {
            log.debug("[MAP_POST] Member uid={} not online, skip push", memberId);
            continue;
          }

          ch.writeAndFlush(new TextWebSocketFrame(payload));
          log.debug("[MAP_POST] Pushed MAP_POST_NEW to uid={}", memberId);
        } catch (Exception e) {
          log.warn("[MAP_POST] Failed to push to uid={}: {}", memberId, e.toString());
        }
      }

      log.info("[MAP_POST] Successfully created and pushed mapPostId={} to {} members",
          result.getMapPostId(), result.getMemberIds().size());

    } catch (IllegalArgumentException bad) {
      // 业务性错误（参数不全、验证失败）
      var err = om.createObjectNode();
      err.put("type", "ERROR");
      err.put("msg", bad.getMessage() == null ? "Invalid request" : bad.getMessage());
      ctx.writeAndFlush(new TextWebSocketFrame(err.toString()));
      log.warn("[MAP_POST] Validation failed for uid={}: {}", uid, bad.getMessage());
    } catch (Exception e) {
      log.error("[MAP_POST] Creation failed for uid=" + uid, e);
      ctx.writeAndFlush(new TextWebSocketFrame(
          "{\"type\":\"ERROR\",\"msg\":\"Server error while creating map post\"}"));
    }

  }

}
