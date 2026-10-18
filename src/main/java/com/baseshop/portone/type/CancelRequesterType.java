package com.baseshop.portone.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CancelRequesterType {

    CUSTOMER("구매자"),
    ADMIN("관리자");


    private final String description;
}
