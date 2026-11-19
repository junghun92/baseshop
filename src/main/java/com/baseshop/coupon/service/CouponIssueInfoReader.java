package com.baseshop.coupon.service;

import com.baseshop.common.config.CacheConfig;
import com.baseshop.common.exception.BusinessException;
import com.baseshop.common.exception.ErrorCode;
import com.baseshop.coupon.dto.CouponIssueInfo;
import com.baseshop.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssueInfoReader {

    private final CouponRepository couponRepository;

    /** 발급용 쿠폰 정보 조회 (캐시 우선, 없으면 DB 조회 후 저장) */
    @Cacheable(cacheNames = CacheConfig.COUPON_ISSUE, key = "#id", sync = true)
    public CouponIssueInfo read(Long id) {
        return couponRepository.findById(id)
                .map(CouponIssueInfo::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

}
