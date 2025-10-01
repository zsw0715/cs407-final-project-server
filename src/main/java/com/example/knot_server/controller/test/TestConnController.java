package com.example.knot_server.controller.test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试短连接
 */
@RestController
@RequestMapping("/test")
public class TestConnController {
    @RequestMapping("/")
    public String testConnection() {
        return "Connection Successful!";
    }
}
