package com.baseshop.coupon.repository;

import com.baseshop.common.exception.BusinessException;
import com.baseshop.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CouponIssuedRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> script = RedisScript.of(
            new ClassPathResource("lua/issue.lua"),
            Long.class
    );

    public void initStock(long couponId, int totalQuantity) {
        redisTemplate.opsForValue().set(stockKey(couponId), String.valueOf(totalQuantity));
    }

    public void tryIssue(long couponId, long userId) {
        Long result = redisTemplate.execute(
                script,
                List.of(stockKey(couponId), membersKey(couponId)),
                String.valueOf(userId)
        );
        if (result == null) {
            throw new IllegalStateException("Lua 스크립트 결과가 null");
        }

        switch (result.intValue()) {
            case 1 -> {} // 성공
            case 0 -> throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
            case -1 -> throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
            default -> throw new IllegalStateException("예상치 못한 Lua 결과: " + result);
        }
    }

    private String stockKey(long couponId) {
        return "coupon:" + couponId + ":stock";
    }

    private String membersKey(long couponId) {
        return "coupon:" + couponId + ":members";
    }

}
