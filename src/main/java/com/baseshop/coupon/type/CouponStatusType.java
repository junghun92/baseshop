package com.baseshop.coupon.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponStatusType {

    ACTIVE("발급 가능"),
    STOPPED("발급 중지");

    private final String description;


}
