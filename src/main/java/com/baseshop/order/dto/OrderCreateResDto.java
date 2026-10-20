package com.baseshop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
public class OrderCreateResDto {

    @Schema(description = "주문번호")
    private Long orderId;

    @Schema(description = "포트원 결제 ID (requestPayment의 paymentId로 사용)")
    private String portonePaymentId;

    @Schema(description = "주문명")
    private String orderName;

    @Schema(description = "총 주문금액")
    private long totalAmount;

    @Builder
    public OrderCreateResDto(Long orderId, String portonePaymentId, String orderName, long totalAmount) {
        this.orderId = orderId;
        this.portonePaymentId = portonePaymentId;
        this.orderName = orderName;
        this.totalAmount = totalAmount;
    }
}
