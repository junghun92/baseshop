package com.baseshop.order.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatusType {

    PENDING("주문대기"),
    COMPLETED("주문완료"),
    CANCELLED("주문취소"),
    FAILED("주문실패");

    private final String description;


}
