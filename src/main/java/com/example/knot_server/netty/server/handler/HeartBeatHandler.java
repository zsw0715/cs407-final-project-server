package com.example.knot_server.netty.server.handler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.SessionKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ChannelHandler.Sharable
public class HeartBeatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ObjectMapper om;

    private final StringRedisTemplate redis;

    private static final int ONLINE_TTL_SECONDS = 90;

    public HeartBeatHandler(ObjectMapper om, StringRedisTemplate redis) {
        this.om = om;
        this.redis = redis;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String text = msg.text();
        JsonNode n = om.readTree(text);

        if (!n.hasNonNull("type") || !"HEARTBEAT".equalsIgnoreCase(n.get("type").asText())) {
            ctx.fireChannelRead(msg.retain());
            return;
        }

        Long uid = ctx.channel().attr(ChannelAttrs.UID).get();
        if (uid == null) {
            // 未认证也发了心跳——直接丢或返回错误
            ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"not authed\"}"));
            return;
        }

        final String onKey = SessionKeys.onlineKey(uid);

        // 1) 读取刷新前的剩余 TTL（秒）
        Long remain = redis.getExpire(onKey, TimeUnit.SECONDS);
        // 规范化：Redis 可能返回 null（驱动差异）或 -2（不存在）或 -1（无过期）
        long remainSec = (remain == null || remain < 0) ? 0L : remain;

        // 2) 刷新在线 TTL（重置为 ONLINE_TTL）
        redis.opsForValue().set(onKey, "1", Duration.ofSeconds(ONLINE_TTL_SECONDS));

        // 3) 回 ACK，带上刷新前的剩余时间，以及标准 TTL，便于前端显示/校准
        ObjectNode ack = om.createObjectNode();
        ack.put("type", "HEARTBEAT_ACK");
        ack.put("uid", uid);
        ack.put("remain", remainSec); // 刷新前还剩多少秒
        ack.put("ttl", ONLINE_TTL_SECONDS); // 标准 TTL（90）
        ack.put("serverTs", System.currentTimeMillis());

        ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(ack)));
    }

}
