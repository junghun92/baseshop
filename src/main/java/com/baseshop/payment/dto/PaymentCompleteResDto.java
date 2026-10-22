package com.baseshop.payment.dto;

import com.baseshop.order.entity.Order;
import com.baseshop.order.type.OrderStatusType;
import com.baseshop.payment.entity.PayMethodType;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.type.PaymentStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentCompleteResDto {

    @Schema(description = "주문번호")
    private final Long orderId;

    @Schema(description = "주문상태")
    private final OrderStatusType orderStatusType;

    @Schema(description = "결제상태")
    private final PaymentStatusType paymentStatusType;

    @Schema(description = "결제금액")
    private final Long amount;

    @Schema(description = "결제수단")
    private final PayMethodType payMethodType;

    @Schema(description = "결제완료 일시 (결제완료일 때만)")
    private final LocalDateTime paidDate;

    @Schema(description = "고객 안내 메시지")
    private final String message;

    private PaymentCompleteResDto(Order order, Payment payment) {
        this.orderId = order.getId();
        this.orderStatusType = order.getOrderStatusType();
        this.paymentStatusType = payment.getPaymentStatusType();
        this.amount = payment.getAmount();
        this.payMethodType = payment.getPayMethodType();
        this.paidDate = payment.getPaidDate();
        this.message = createMessage(payment);
    }

    public static PaymentCompleteResDto of(Order order, Payment payment) {
        return new PaymentCompleteResDto(order, payment);
    }

    private static String createMessage(Payment payment) {
        return switch (payment.getPaymentStatusType()) {
            case PAID -> "결제가 완료되었습니다.";
            case FAILED -> payment.getFailReason() != null ? payment.getFailReason() : "결제에 실패했습니다.";
            case CANCELLED -> "결제가 취소되었습니다. 결제하신 금액은 환불됩니다.";
            default -> "결제 처리 중입니다.";
        };
    }

    public boolean isReady() {
        return paymentStatusType == PaymentStatusType.READY;
    }

    public boolean isPaid() {
        return paymentStatusType == PaymentStatusType.PAID;
    }
}
