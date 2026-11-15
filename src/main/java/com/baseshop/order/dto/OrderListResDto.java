package com.baseshop.order.dto;

import com.baseshop.order.entity.Order;
import com.baseshop.order.type.OrderStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrderListResDto {

    @Schema(description = "주문번호")
    private Long id;

    @Schema(description = "상품 금액 합계")
    private long totalPrice;

    @Schema(description = "쿠폰 할인 금액 (쿠폰 안 쓰면 0)")
    private long discountAmount;

    @Schema(description = "최종 결제 금액 (상품 금액 - 쿠폰 할인)")
    private long paymentAmount;
    
    @Schema(description = "주문상태 (ORDER: 주문, CANCEL: 취소)", example = "ORDER")
    private OrderStatusType orderStatusType;

    @Schema(description = "주문상품 리스트")
    private List<OrderItemListResDto> orderItemList = new ArrayList<>();

    @Schema(description = "주문일")
    private LocalDateTime orderDate;

    public OrderListResDto(Order order) {
        this.id = order.getId();
        this.totalPrice = order.getTotalPrice();
        this.discountAmount = order.getDiscountAmount();
        this.paymentAmount = order.getPaymentAmount();
        this.orderStatusType = order.getOrderStatusType();
        this.orderDate = order.getRegDate();
        this.orderItemList = order.getOrderItemList().stream().map(OrderItemListResDto::new).toList();
    }
}
