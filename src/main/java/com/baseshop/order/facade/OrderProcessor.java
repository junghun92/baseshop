package com.baseshop.order.facade;

import com.baseshop.order.entity.Order;
import com.baseshop.order.service.OrderService;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.service.PaymentService;
import com.baseshop.payment.type.PaymentStatusType;
import com.baseshop.portone.dto.PortOneCancelResDto;
import com.baseshop.portone.type.CancelRequesterType;
import com.baseshop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProcessor {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ProductService productService;

    /**
     * 주문취소 DB 반영
     * - DB 반영 전 웹훅이 먼저 취소를 반영했으면 처리하지 않는다.
     */
    @Transactional
    public void updateCancel(String portonePaymentId,
                             CancelRequesterType cancelRequesterType,
                             PortOneCancelResDto.Cancellation cancellation) {
        // 1. 결제 락 + 상태 재확인
        Payment payment = paymentService.getPaymentForUpdate(portonePaymentId);
        if (payment.getPaymentStatusType() != PaymentStatusType.PAID) {
            log.info("이미 취소 반영된 결제 portonePaymentId={}, status={}",
                    portonePaymentId, payment.getPaymentStatusType());
            return;
        }

        // 2. 결제취소
        payment.cancel(
                cancelRequesterType,
                cancellation.getTotalAmount(),
                cancellation.getReason(),
                cancellation.getCancelledDate());

        // 3. 주문취소
        Order order = payment.getOrder();
        order.cancel();

        // 4. 재고복구
        productService.updateStockRestore(order.getOrderItemList());
    }

}
