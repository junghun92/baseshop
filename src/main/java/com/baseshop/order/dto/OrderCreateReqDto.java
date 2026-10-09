package com.baseshop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderCreateReqDto {

    @Schema(description = "주문할 상품 목록")
    List<OrderItemCreateReqDto> items = new ArrayList<>();

}
