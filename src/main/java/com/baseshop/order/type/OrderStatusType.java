package com.baseshop.order.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum OrderStatusType {

    PENDING("주문대기"),
    COMPLETED("주문완료"),
    CANCELLED("주문취소"),
    FAILED("주문실패");

    private final String description;

    // 주문내역에 보여줄 상태
    public static final List<OrderStatusType> VISIBLE_STATUSES = List.of(COMPLETED, CANCELLED);

    /**
     * 주문 상태 전이 규칙
     * - PENDING   -> COMPLETED (결제완료), FAILED (결제실패, 금액 불일치, 만료)
     * - COMPLETED -> CANCELLED (고객 주문취소, 콘솔 취소 동기화)
     * - CANCELLED, FAILED -> 끝 (어디로도 못 감 -> 중복 취소, 재고 중복 복구 방지)
     */
    public boolean canTransitTo(OrderStatusType target) {
        return switch (this) {
            case PENDING -> target == COMPLETED || target == FAILED;
            case COMPLETED -> target == CANCELLED;
            case CANCELLED, FAILED -> false;
        };
    }

}
