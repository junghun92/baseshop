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

}
