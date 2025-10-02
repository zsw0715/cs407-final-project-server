package com.example.knot_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis 脚本配置。
 * 提供一个 compareAndDelScript：当且仅当 key 当前存的值等于传入的 channelId 时，删除该 key。
 * 返回 1 表示删除成功（条件满足且已删除），否则返回 0。
 */
@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<Long> compareAndDelScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // KEYS[1] = 需要校验并删除的 key
        // ARGV[1] = 期待匹配的 channelId
        script.setScriptText(
                "local v = redis.call('GET', KEYS[1]);" +
                " if v == ARGV[1] then" +
                "   redis.call('DEL', KEYS[1]);" +
                "   return 1" +
                " else" +
                "   return 0" +
                " end"
        );
        script.setResultType(Long.class);
        return script;
    }
}
