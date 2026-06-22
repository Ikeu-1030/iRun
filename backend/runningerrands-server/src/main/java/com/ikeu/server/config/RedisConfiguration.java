package com.ikeu.server.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置，配置 RedisTemplate 序列化方式和多 TTL 的 RedisCacheManager。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@Configuration
public class RedisConfiguration {

    /**
     * 创建 RedisTemplate Bean，设置 key 为 String 序列化。
     *
     * <p>使用 StringRedisSerializer 序列化 key，确保 Redis 中 key 可读。
     * value 使用默认的 JDK 序列化，适用于存储简单字符串（如 token、验证码）。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建Redis模板对象...");
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    /**
     * 配置 Spring Cache 的 RedisCacheManager，按缓存名称设置不同的 TTL。
     *
     * <p>使用 Jackson2JsonRedisSerializer 序列化缓存 value，支持复杂对象缓存。
     * 各缓存 TTL：
     * <ul>
     *   <li>task:hall — 2分钟</li>
     *   <li>task:detail — 10分钟</li>
     *   <li>runner:leaderboard — 5分钟</li>
     *   <li>admin:dashboard — 3分钟</li>
     *   <li>默认 — 10分钟（TASK_CACHE_EXPIRE）</li>
     * </ul>
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisCacheManager 实例
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建Redis缓存管理器...");

        JacksonObjectMapper mapper = new JacksonObjectMapper();
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ikeu.model.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(RedisConstant.TASK_CACHE_EXPIRE))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> cacheName + ":");

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(RedisConstant.CACHE_TASK_HALL,
                defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigs.put(RedisConstant.CACHE_TASK_DETAIL,
                defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(RedisConstant.CACHE_LEADERBOARD,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(RedisConstant.CACHE_DASHBOARD,
                defaultConfig.entryTtl(Duration.ofMinutes(3)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
