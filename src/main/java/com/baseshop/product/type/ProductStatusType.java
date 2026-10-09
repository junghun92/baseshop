package com.baseshop.product.type;

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
public enum ProductStatusType {

    SALE("SALE", "판매중"),
    SOLD_OUT("SOLD_OUT", "품절"),
    STOP("STOP", "판매중지");

    private final String value;
    private final String description;

    @JsonValue
    public String getValue(){
        return value;
    }

    private static final Map<String, ProductStatusType> ProductStatusTypeStringToEnum =
            Stream.of(values()).collect(Collectors.toMap(ProductStatusType::getValue, obj -> obj));

    @JsonCreator
    public static ProductStatusType fromProductStatusTypeString(final String commandDefineString) {
        return Optional.ofNullable(ProductStatusTypeStringToEnum.get(commandDefineString))
                .orElse(null);
    }

}
