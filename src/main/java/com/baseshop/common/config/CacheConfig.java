package com.baseshop.common.config;

import com.baseshop.coupon.dto.CouponIssueInfo;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String COUPON_ISSUE = "couponIssue";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 기본값: TTL 10분, null 캐시 안 함
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        // 쿠폰 발급 정보: JSON 저장, TTL 10분
        RedisCacheConfiguration couponIssueConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(CouponIssueInfo.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(COUPON_ISSUE, couponIssueConfig))
                .transactionAware()   // 캐시 저장/삭제를 트랜잭션 커밋 이후로
                .build();
    }
}
