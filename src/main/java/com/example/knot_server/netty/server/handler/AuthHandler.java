package com.example.knot_server.netty.server.handler;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.LocalSessionRegistry;
import com.example.knot_server.netty.session.SessionKeys;
import com.example.knot_server.util.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 会话注册表
    private final LocalSessionRegistry registry;

    // Redis 模板
    private final StringRedisTemplate redis;

    // JWT 服务
    private final JwtService jwt;

    // JSON 处理器
    private final ObjectMapper om = new ObjectMapper();

    // 设置在线状态的过期时间为 90 秒
    private static final int ONLINE_TTL_SECONDS = 90;

    // 构造函数注入依赖
    public AuthHandler(LocalSessionRegistry registry, StringRedisTemplate redis, JwtService jwt) {
        this.registry = registry;
        this.redis = redis;
        this.jwt = jwt;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String text = msg.text();
        JsonNode node = om.readTree(text);

        // 非认证消息，传递给下一个处理器
        if (!node.has("type") || !"AUTH".equals(node.get("type").asText())) {
            ctx.fireChannelRead(msg.retain());
            return;
        }

        // 获取 token 字段，如果没有则关闭连接
        String token = node.get("token").asText();
        if (token == null || token.isEmpty()) {
            ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"missing token\"}"));
            ctx.close();
            return;
        }

        try {
            Claims claims = jwt.parse(token).getBody();
            if (!"access".equals(claims.get("typ", String.class))) {
                ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"invalid token type\"}"));
                ctx.close();
                return;
            }

            long uid = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);

            // 绑定到 Channel Attr
            ctx.channel().attr(ChannelAttrs.UID).set(uid);
            ctx.channel().attr(ChannelAttrs.USERNAME).set(username);

            // 写 Redis（全局路由 & 在线状态）
            String chKey = SessionKeys.channelKey(uid);   // "channel:{uid}"
            String onKey = SessionKeys.onlineKey(uid);    // "online:{uid}"
            String chId  = ctx.channel().id().asShortText();

            redis.opsForValue().set(chKey, chId);
            // 2) 在线表：带 TTL
            redis.opsForValue().set(onKey, "827", Duration.ofSeconds(ONLINE_TTL_SECONDS));

            // 本机注册（含顶号逻辑）
            registry.register(ctx.channel(), uid);

            // 认证成功
            ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"AUTH_OK\",\"uid\":" + uid + "}"));

        } catch (Exception e) {
            ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"auth failed\"}"));
            ctx.close();
        }
    }

}
