package com.baseshop.payment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum PayMethodType {

    CARD("카드", "PaymentMethodCard"),
    EASY_PAY("간편결제", "PaymentMethodEasyPay"),
    TRANSFER("계좌이체", "PaymentMethodTransfer"),
    VIRTUAL_ACCOUNT("가상계좌", "PaymentMethodVirtualAccount"),
    MOBILE("휴대폰결제", "PaymentMethodMobile"),
    GIFT_CERTIFICATE("상품권", "PaymentMethodGiftCertificate"),
    CONVENIENCE_STORE("편의점", "PaymentMethodConvenienceStore"),
    CRYPTO("암호화폐", "PaymentMethodCrypto"),
    ETC("기타", null);

    private final String description;
    private final String portOneType;   // 포트원 응답의 method.type 값

    private static final Map<String, PayMethodType> PORTONE_TYPE_MAP = Arrays.stream(values())
            .filter(type -> type.portOneType != null)
            .collect(Collectors.toMap(PayMethodType::getPortOneType, type -> type));

    // 포트원 결제수단 -> 우리 결제수단 (없거나 모르는 값이면 ETC)
    public static PayMethodType fromPortOne(String portOneType) {
        return PORTONE_TYPE_MAP.getOrDefault(portOneType, ETC);
    }
}
