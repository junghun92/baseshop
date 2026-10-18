package com.baseshop.portone.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CancelReasonType {

    CUSTOMER_REQUEST("고객 요청"),
    ADMIN_CANCEL("관리자 취소"),
    AMOUNT_MISMATCH("결제 금액 불일치"),
    EXTERNAL_SYNC("포트원 결제사 취소 동기화");

    private final String description;

}
