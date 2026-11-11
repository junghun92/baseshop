package com.baseshop.coupon.service.issue;

public interface CouponIssuedProcessor {

    Long createIssuedCoupon(Long id, Long memberId);
}
