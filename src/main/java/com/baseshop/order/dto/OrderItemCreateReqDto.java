package com.baseshop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OrderItemCreateReqDto {

    @Schema(description = "상품번호", example = "1")
    private Long productId;

    @Schema(description = "주문 수량", example = "2")
    private int quantity;

    @Schema(description = "장바구니 번호 (장바구니에서 주문할 때만, 바로 구매는 생략)", example = "1")
    private Long cartId;
}
