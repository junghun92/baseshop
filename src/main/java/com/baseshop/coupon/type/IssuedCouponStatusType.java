package com.baseshop.coupon.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssuedCouponStatusType {
    
    ISSUED("사용 가능"),
    USED("사용 완료"),
    EXPIRED("사용 기간 만료");

    private final String description;

    public boolean canTransitTo(IssuedCouponStatusType target) {
        return switch (this) {
            case ISSUED -> target == USED || target == EXPIRED;
            case USED -> target == ISSUED; // 결제실패, 주문 취소 시 복구
            case EXPIRED -> false;
        };
    }


}
