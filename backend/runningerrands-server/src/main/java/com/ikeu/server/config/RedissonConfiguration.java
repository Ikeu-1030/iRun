package com.ikeu.server.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置，创建单机 Redis 连接的 RedissonClient Bean。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@Configuration
public class RedissonConfiguration {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.database:1}")
    private int redisDatabase;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 创建 Redisson 客户端 Bean，连接单机 Redis 服务。
     *
     * <p>使用 application.yml 中配置的 host、port、password、database 参数
     * 构造单机模式 RedissonClient，用于分布式锁等高级 Redis 操作。
     *
     * @return RedissonClient 实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        log.info("开始创建RedissonClient对象...");
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort)
                .setDatabase(redisDatabase);
                // Redis 密码认证（如 Redis 设置了密码请取消注释）
                // .setPassword(redisPassword);
        return Redisson.create(config);
    }
}

