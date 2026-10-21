package com.baseshop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderCreateReqDto {

    @Schema(description = "주문할 상품 목록")
    @NotEmpty(message = "주문할 상품이 없습니다.")
    private List<OrderItemCreateReqDto> itemList = new ArrayList<>();

    @Data
    public static class OrderItemCreateReqDto {

        @Schema(description = "상품번호", example = "1")
        private Long productId;

        @Schema(description = "주문 수량", example = "2")
        private int quantity;
    }
}
