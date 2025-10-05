package com.example.knot_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Boot 配置类 + 全局 Bean 注册
 * 
 * 注解：
 *      当 Spring Boot 启动时，它会扫描所有带有 @Configuration 的类；
 *      然后自动执行其中被 @Bean 标注的方法；
 * 	    把这些返回的对象 放进全局的 Spring 容器（ApplicationContext） 中。
 */
@Configuration
public class AppBeans {
    
    /** 注册一个密码加密器 Bean */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
