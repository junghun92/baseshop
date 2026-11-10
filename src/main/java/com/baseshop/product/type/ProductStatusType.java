package com.baseshop.product.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ProductStatusType {

    SALE("판매중"),
    SOLD_OUT("품절"),
    STOP("판매중지");


    public static final List<ProductStatusType> USER_VISIBLE = List.of(SALE, SOLD_OUT);
    private final String description;


}
