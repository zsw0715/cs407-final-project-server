package com.example.knot_server.netty.session;

import io.netty.util.AttributeKey;

/**
 * Channel attribute keys for storing user-related information in Netty channels.
 */
public final class ChannelAttrs {
    public static final AttributeKey<Long> UID = AttributeKey.valueOf("uid");
    public static final AttributeKey<String> USERNAME = AttributeKey.valueOf("username");
    private ChannelAttrs() {}
}
