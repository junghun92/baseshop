package com.baseshop.security.token.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RefreshTokenRepository(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-token-expire-time}") long refreshTokenExpireTime) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMillis(refreshTokenExpireTime);
    }

    /** 저장 (같은 아이디면 덮어씀) */
    public void save(String username, String refreshToken){
        redisTemplate.opsForValue().set(KEY_PREFIX + username, refreshToken, ttl);
    }

    /** 조회 */
    public Optional<String> findByUsername(String username){
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + username));
    }

    /** 삭제 */
    public void deleteByUsername(String username){
        redisTemplate.delete(KEY_PREFIX + username);
    }
}
