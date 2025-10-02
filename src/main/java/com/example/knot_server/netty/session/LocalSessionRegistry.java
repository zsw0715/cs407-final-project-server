package com.example.knot_server.netty.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import io.netty.channel.Channel;

import org.springframework.stereotype.Component;

/**
 * Local session registry for managing active user sessions in memory.
 */
@Component
public class LocalSessionRegistry {

    // uid -> Channel：面向业务的推送与单点登录
    private final ConcurrentMap<String, Channel> channelsById = new ConcurrentHashMap<>();

    // channelId -> Channel：面向底层路由与资源管理（跨节点落地、强制下线、回调清理）
    private final ConcurrentMap<Long, Channel> channelsByUid = new ConcurrentHashMap<>();

    /**
     * Register a new channel with the given user ID.
     * 
     * @param ch
     * @param uid
     */
    public void register(Channel ch, long uid) {
        channelsById.put(ch.id().asShortText(), ch);
        Channel old = channelsByUid.put(uid, ch);
        ch.closeFuture().addListener(f -> unregister(ch));
        if (old != null && old != ch) {
            old.close();
        }
    }

    public void unregister(Channel ch) {
        channelsById.remove(ch.id().asShortText());
        Long uid = ch.attr(ChannelAttrs.UID).get();
        // 仅在当前通道仍然是该用户的活动通道时移除
        if (uid != null) {
            channelsByUid.compute(uid, (k, v) -> v == ch ? null : v);
        }
    }

    public Channel byUid(long uid) {
        return channelsByUid.get(uid);
    }

    public Channel byChannelId(String id) {
        return channelsById.get(id);
    }

}
