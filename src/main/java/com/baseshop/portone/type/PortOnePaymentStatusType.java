package com.baseshop.portone.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PortOnePaymentStatusType {

    CANCELLED("결제취소"),
    FAILED("결제실패"),
    PAID("결제완료"),
    PARTIAL_CANCELLED("결제 부분취소"),
    PAY_PENDING("결제 완료대기"),
    READY("준비상태"),
    VIRTUAL_ACCOUNT_ISSUED("가상계좌 발급완료");

    private final String description;
}
