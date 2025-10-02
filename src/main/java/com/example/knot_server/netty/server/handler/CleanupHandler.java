package com.example.knot_server.netty.server.handler;

import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.LocalSessionRegistry;
import com.example.knot_server.netty.session.SessionKeys;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ChannelHandler.Sharable
public class CleanupHandler extends ChannelDuplexHandler {

    private final LocalSessionRegistry registry;
    private final StringRedisTemplate redis;
    private final RedisScript<Long> compareAndDelScript;

    // compareAndDelScript 语义：
    // if (GET key == argChannelId) DEL key; return 1/0
    public CleanupHandler(LocalSessionRegistry registry,
            StringRedisTemplate redis,
            RedisScript<Long> compareAndDelScript) {
        this.registry = registry;
        this.redis = redis;
        this.compareAndDelScript = compareAndDelScript;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 读空闲超时，主动断开（触发 channelInactive → 本机与 Redis 清理）
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(io.netty.channel.ChannelHandlerContext ctx) throws Exception {
        try {
            final Long uid = ctx.channel().attr(ChannelAttrs.UID).get();
            final String channelId = ctx.channel().id().asShortText();

            // 1) 本机清理
            registry.unregister(ctx.channel());

            // 2) Redis 清理：仅当当前路由仍指向本次断开的 channelId 时删除（防止把新连接误删）
            if (uid != null) {
                final String chKey = SessionKeys.channelKey(uid);
                try {
                    redis.execute(compareAndDelScript, Collections.singletonList(chKey), channelId);
                } catch (Exception ex) {
                    log.warn("compareAndDel failed for key={}, channelId={}, err={}", chKey, channelId, ex.toString());
                }
                // online:{uid} delete 
                redis.delete(SessionKeys.onlineKey(uid));
            }
        } finally {
            super.channelInactive(ctx);
        }
    }

}
