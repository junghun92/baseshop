package com.baseshop.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class IssuedCouponCountDto {

    private Long couponId;
    private Long issuedCount;

    public IssuedCouponCountDto(Long couponId, Long issuedCount) {
        this.couponId = couponId;
        this.issuedCount = issuedCount;
    }
}
