package com.baseshop.order.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum OrderStatusType {

    ORDER("ORDER", "주문완료"),
    CANCEL("CANCEL", "취소완료");

    private final String value;
    private final String description;

    @JsonValue
    public String getValue(){
        return value;
    }

    private static final Map<String, OrderStatusType> OrderStatusTypeStringToEnum =
            Stream.of(values()).collect(Collectors.toMap(OrderStatusType::getValue, obj -> obj));

    @JsonCreator
    public static OrderStatusType fromOrderStatusTypeString(final String commandDefineString) {
        return Optional.ofNullable(OrderStatusTypeStringToEnum.get(commandDefineString))
                .orElse(null);
    }

}
