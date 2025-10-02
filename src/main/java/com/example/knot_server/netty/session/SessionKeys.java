package com.example.knot_server.netty.session;

/**
 * Session key utilities for managing user sessions in Redis.
 */
public final class SessionKeys {

    /**
     * 频道键
     * @param uid 用户ID
     * @return 频道键
     */
    public static String channelKey(long uid) {
        return "channel:" + uid;
    }

    /**
     * 在线用户键
     * @param uid 用户ID
     * @return 在线用户键
     */
    public static String onlineKey(long uid) {
        return "online:" + uid;
    }

    /**
     * 防止类被实例化
     */
    private SessionKeys() {
    }
}
