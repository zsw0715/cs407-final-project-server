package com.example.knot_server.controller.test;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试Redis连接
 */
@RestController
@RequestMapping("/test")
public class TestRedisController {
    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_KEY = "test_key";
    private static final String REDIS_VALUE = "Hello, Redis!";

    public TestRedisController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RequestMapping("/redis")
    public String testRedis() {
        redisTemplate.opsForValue().set(REDIS_KEY, REDIS_VALUE);
        return "Stored and retrieved from Redis: " + redisTemplate.opsForValue().get(REDIS_KEY);
    }

}
