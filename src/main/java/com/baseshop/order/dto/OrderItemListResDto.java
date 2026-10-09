package com.baseshop.order.dto;

import com.baseshop.order.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OrderItemListResDto {

    @Schema(description = "주문 상품번호")
    private Long id;

    @Schema(description = "상품 고유번호")
    private Long productId;

    @Schema(description = "상품명")
    private String productName;

    @Schema(description = "상품가격")
    private int orderPrice;

    @Schema(description = "상품수량")
    private int quantity;

    public OrderItemListResDto(OrderItem orderItem) {
        this.id = orderItem.getId();
        this.productId = orderItem.getProduct().getId();
        this.productName = orderItem.getProduct().getName();
        this.orderPrice = orderItem.getOrderPrice();
        this.quantity = orderItem.getQuantity();
    }
}
