package com.example.knot_server.netty.server.handler;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.LocalSessionRegistry;
import com.example.knot_server.netty.session.SessionKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ChannelHandler.Sharable
public class LogoutHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final StringRedisTemplate redis;

    private final LocalSessionRegistry registry;

    private final RedisScript<Long> compareAndDelScript;

    public LogoutHandler(StringRedisTemplate redis, LocalSessionRegistry registry,
            RedisScript<Long> compareAndDelScript) {
        this.redis = redis;
        this.registry = registry;
        this.compareAndDelScript = compareAndDelScript;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        JsonNode node = new ObjectMapper().readTree(msg.text());

        if (!node.has("type") || !"LOGOUT".equals(node.get("type").asText())) {
            ctx.fireChannelRead(msg.retain());
            return;
        }

        Long uid = ctx.channel().attr(ChannelAttrs.UID).get();
        if (uid == null) {
            ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"not authed\"}"));
            return;
        }

        String chKey = SessionKeys.channelKey(uid);
        String onKey = SessionKeys.onlineKey(uid);
        String chId = ctx.channel().id().asShortText();

        // 1) 原子删除 channel:{uid}（若仍指向本 channel）
        redis.execute(compareAndDelScript, java.util.Collections.singletonList(chKey), chId);
        // 2) 立刻删除 online:{uid}
        redis.delete(onKey);

        // 3) 本机清理并断开
        registry.unregister(ctx.channel());
        ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"LOGOUT_OK\"}"))
           .addListener(f -> ctx.close());
    }

}
