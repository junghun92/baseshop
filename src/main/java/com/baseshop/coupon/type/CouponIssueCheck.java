package com.baseshop.coupon.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 쿠폰 발급 가능 여부를 나타내는 Enum 클래스입니다.
 */
@Getter
@RequiredArgsConstructor
public enum CouponIssueCheck {

    AVAILABLE("발급 가능"),
    ISSUED("발급 받음"),
    UNAVAILABLE("발급 불가"),
    UPCOMING("오픈 예정"),
    SOLD_OUT("매진");

    private final String description;
}
