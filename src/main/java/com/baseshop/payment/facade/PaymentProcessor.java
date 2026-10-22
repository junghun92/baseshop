package com.baseshop.payment.facade;

import com.baseshop.cart.service.CartService;
import com.baseshop.order.entity.Order;
import com.baseshop.order.type.OrderStatusType;
import com.baseshop.payment.dto.PaymentCompleteResDto;
import com.baseshop.payment.entity.PayMethodType;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.service.PaymentService;
import com.baseshop.portone.PortOneClient;
import com.baseshop.portone.dto.PortOneCancelReqDto;
import com.baseshop.portone.dto.PortOneCancelResDto;
import com.baseshop.portone.dto.PortOnePaymentResDto;
import com.baseshop.portone.type.CancelReasonType;
import com.baseshop.portone.type.CancelRequesterType;
import com.baseshop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    private final PaymentService paymentService;
    private final ProductService productService;
    private final CartService cartService;
    private final PortOneClient portOneClient;

    /** 포트원 결제 결과로 동기화 (결제완료, 웹훅 공통) */
    @Transactional
    public PaymentCompleteResDto updateSync(String portonePaymentId, PortOnePaymentResDto portOnePayment) {
        Payment payment = paymentService.getPaymentForUpdate(portonePaymentId);
        Order order = payment.getOrder();
        String portOneMethodType = portOnePayment.getMethodType();
        String failReason = portOnePayment.getFailReason();

        // payment 결제 상태
        switch (payment.getPaymentStatusType()){
            case READY -> syncReady(portOnePayment, payment, order, portOneMethodType, failReason);
            case PAID -> syncPaid(portOnePayment, payment, order);
            default -> {} // CANCELLED, FAILED: 끝난 결제라 변동 없음
        }

        return PaymentCompleteResDto.of(payment.getOrder(), payment);
    }

    /** 결제 DB READY: 포트원 결제 상태로 반영 */
    private void syncReady(PortOnePaymentResDto portOnePayment, Payment payment, Order order, String portOneMethodType, String failReason) {
        // portOne 결제상태
        switch (portOnePayment.getStatus()){
            case PAID -> { // 정상 완료: 결제완료, 주문완료, 장바구니에서 주문상품 삭제
                if(portOnePayment.isAmountMismatch(payment.getAmount())) {
                    refundAmountMismatch(payment, order); // 금액 불일치: 환불, 결제취소, 주문실패, 재고복구
                } else {
                    // 결제완료 -> 주문완료 -> 장바구니 삭제
                    payment.complete(
                            PayMethodType.fromPortOne(portOneMethodType),
                            portOnePayment.getTransactionId(),
                            portOnePayment.getPaidDate());
                    order.transitTo(OrderStatusType.COMPLETED);
                    deleteCartItemList(order);
                }
            }
            case FAILED -> {
                // 결제실패 -> 주문실패 -> 재고복구
                payment.fail(failReason, portOnePayment.getFailedDate());
                order.transitTo(OrderStatusType.FAILED);
                restoreStock(order);
            }
            case CANCELLED -> {
                // 결제 전 취소 (금액 불일치 환불 후 DB 반영 실패 등): 결제취소 -> 주문실패 -> 재고복구
                payment.cancel(
                        CancelRequesterType.ADMIN,
                        payment.getAmount(),
                        CancelReasonType.EXTERNAL_SYNC.getDescription(),
                        portOnePayment.getCancelledDate());
                order.transitTo(OrderStatusType.FAILED);
                productService.updateStockRestore(order.getOrderItemList());
            }
            default -> {} // 결제 진행중 (결제창 닫음 등) -> 결과 나오면 웹훅이 처리
        }
    }

    /** 결제 DB PAID: 포트원 결제 상태로 반영 */
    private void syncPaid(PortOnePaymentResDto portOnePayment, Payment payment, Order order) {
        // portOne 결제상태
        // 결제 후 취소 (콘솔 취소, 주문취소 환불 후 DB 반영 실패)
        if (portOnePayment.isCancelled()) {
            // 결제취소 → 주문취소 → 재고복구
            payment.cancel(
                    CancelRequesterType.ADMIN,
                    payment.getAmount(),
                    CancelReasonType.EXTERNAL_SYNC.getDescription(),
                    portOnePayment.getCancelledDate());
            order.transitTo(OrderStatusType.CANCELLED);
            restoreStock(order);
        }
    }

    /**
     * 금액 불일치 환불 (위변조 의심): 포트원 결제취소 -> 결제취소 -> 주문실패 -> 재고복구
     * - 드문 경우라 트랜잭션 안에서 포트원 환불까지 처리한다.
     * - 환불 실패: 예외 -> 롤백 -> READY 유지 -> 다음 웹훅에서 재시도
     * - 환불 성공 후 DB 실패: Cancelled 웹훅 -> READY + CANCELLED로 정리
     */
    private void refundAmountMismatch(Payment payment, Order order) {
        log.warn("결제 금액 불일치 환불 요청 portonePaymentId={}, 주문금액={}",
                payment.getPortonePaymentId(), payment.getAmount());

        PortOneCancelReqDto cancelReqDto = new PortOneCancelReqDto(
                CancelReasonType.AMOUNT_MISMATCH.getDescription(), CancelRequesterType.ADMIN);
        PortOneCancelResDto.Cancellation cancellation =
                portOneClient.cancel(payment.getPortonePaymentId(), cancelReqDto);
        
        // 결제취소
        payment.cancel(
                CancelRequesterType.ADMIN,
                cancellation.getTotalAmount(),
                cancellation.getReason(),
                cancellation.getCancelledDate());
        
        // 주문실패
        order.transitTo(OrderStatusType.FAILED);
        
        // 재고복구
        restoreStock(order);
    }

    /** 주문 상품 재고 복구 */
    private void restoreStock(Order order) {
        productService.updateStockRestore(order.getOrderItemList());
    }

    /** 결제 완료 후 장바구니에서 주문 상품 삭제 */
    private void deleteCartItemList(Order order) {
        cartService.deleteOrderItemList(order.getMember().getId(), order.getProductIdList());
    }

    /**
     * 결제 만료 처리 (결제창만 열고 결제 안 함 등)
     * - 결제 행 락 → READY 재확인 (그 사이 /complete, 웹훅이 처리했으면 종료)
     */
    @Transactional
    public void updateExpire(String portonePaymentId) {
        Payment payment = paymentService.getPaymentForUpdate(portonePaymentId);
        if(!payment.isReady()){
            return;
        }

        Order order = payment.getOrder();

        // 결제실패 -> 주문실패 -> 재고복구
        payment.fail("결제시간 만료 (10분)", LocalDateTime.now());
        order.transitTo(OrderStatusType.FAILED);
        productService.updateStockRestore(order.getOrderItemList());

        log.info("결제 만료 처리완료 portonePaymentId={}", portonePaymentId);
    }

}
