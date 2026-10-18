package com.baseshop.payment.service;

import com.baseshop.cart.repository.CartRepository;
import com.baseshop.order.entity.Order;
import com.baseshop.payment.dto.PaymentCompleteResDto;
import com.baseshop.payment.entity.PayMethodType;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.repository.PaymentRepository;
import com.baseshop.payment.type.PaymentStatusType;
import com.baseshop.portone.PortOneClient;
import com.baseshop.portone.dto.PortOneCancelReqDto;
import com.baseshop.portone.dto.PortOneCancelResDto;
import com.baseshop.portone.dto.PortOnePaymentResDto;
import com.baseshop.portone.type.CancelReasonType;
import com.baseshop.portone.type.CancelRequesterType;
import com.baseshop.portone.type.PortOnePaymentStatusType;
import com.baseshop.product.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final ProductService productService;
    private final PortOneClient portOneClient;

    /**
     * 포트원 결제 결과로 결제 상태를 변경
     */
    @Transactional
    public PaymentCompleteResDto complete(String portonePaymentId, PortOnePaymentResDto portOnePayment) {
        Payment payment = getPaymentForUpdate(portonePaymentId);

        return processComplete(payment, portOnePayment);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentCompleteResDto> getProcessedPaymentResult(Long memberId, String portonePaymentId) {
        Payment payment = paymentRepository.findByPortonePaymentId(portonePaymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보가 없습니다."));

        if (!payment.getOrder().getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("본인의 주문만 결제할 수 있습니다.");
        }

        if (payment.getPaymentStatusType() == PaymentStatusType.READY) {
            return Optional.empty();
        }

        return Optional.of(PaymentCompleteResDto.from(payment.getOrder(), payment));
    }

    /**
     * 결제 조회 (비관적 락)
     * - 프론트 요청과 웹훅이 동시에 와도 한 건씩 처리하도록 함.
     */
    private Payment getPaymentForUpdate(String portonePaymentId) {
        return paymentRepository.findByPortonePaymentIdForUpdate(portonePaymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제정보가 없습니다."));
    }

    /**
     * 결제 검증 및 완료처리
     * - 포트원 결제 상태와 금액을 검증
     * - 처리하지 않는 부분
     *   결제 취소: 예외처리 -> 웹훅 처리
     *   결제 부분취소: 현재 기능없음
     *   준비상태, 결제 완료대기: 결제 진행중으로 판단하고 예외처리 -> 웹훅 처리
     *   가상계좌 발급완료: 현재 기능없음
     */
    private PaymentCompleteResDto processComplete(Payment payment, PortOnePaymentResDto portOnePayment) {
        Order order = payment.getOrder();

        // 이미 처리된 결제면 그대로 반환
        if (payment.getPaymentStatusType() != PaymentStatusType.READY) {
            return PaymentCompleteResDto.from(order, payment);
        }

        // 결제실패 처리
        if (portOnePayment.getStatus() == PortOnePaymentStatusType.FAILED) {
            failPayment(payment, order, portOnePayment);
            return PaymentCompleteResDto.from(order, payment);
        }

        // 결제완료 확인
        if (portOnePayment.getStatus() != PortOnePaymentStatusType.PAID) {
            throw new IllegalArgumentException("결제가 완료되지 않았습니다.");
        }

        // 요청금액과 실제 결제금액이 다르면 위변조로 보고 환불
        if (portOnePayment.getAmount().getTotal() != payment.getAmount()) {
            cancelByAmountMismatch(payment, order);
            return PaymentCompleteResDto.from(order, payment);
        }

        // 결제완료 처리
        completePayment(payment, order, portOnePayment);

        return PaymentCompleteResDto.from(order, payment);
    }

    /** 정상 완료: 결제완료, 주문완료, 장바구니에서 주문상품 삭제 */
    private void completePayment(Payment payment, Order order, PortOnePaymentResDto portOnePayment) {
        String portOneMethodType = portOnePayment.getMethod() == null ? null : portOnePayment.getMethod().getType();

        payment.complete(
                PayMethodType.fromPortOne(portOneMethodType),
                portOnePayment.getTransactionId(),
                portOnePayment.getPaidDate());

        order.complete();

        List<Long> productIdList = order.getOrderItemList().stream()
                .map(orderItem -> orderItem.getProduct().getId())
                .toList();
        cartRepository.deleteByMemberIdAndProductIdList(order.getMember().getId(), productIdList);
    }

    /** 결제실패 : 결제,주문 실패 처리 후, 재고 복구 */
    private void failPayment(Payment payment, Order order, PortOnePaymentResDto portOnePayment) {
        String failReason = portOnePayment.getFailure() == null ? "결제실패" : portOnePayment.getFailure().getReason();

        payment.fail(failReason, portOnePayment.getFailedDate());
        order.fail();
        productService.updateStockRestore(order.getOrderItemList());
    }

    /** 금액 불일치: 포트원 결제취소 요청 후 -> DB 결제취소, 주문실패, 재고복구 */
    private void cancelByAmountMismatch(Payment payment, Order order) {
        log.warn("결제 금액 불일치 환불 요청 portonePaymentId={}, 주문금액={}",payment.getPortonePaymentId(), payment.getAmount());
        cancel(payment, CancelReasonType.AMOUNT_MISMATCH, CancelRequesterType.ADMIN); // 결제취소

        order.fail(); // 주문실패
        productService.updateStockRestore(order.getOrderItemList()); // 재고복구
    }

    /**
     * 결제취소
     */
    public PortOneCancelResDto cancel(Payment payment, CancelReasonType cancelReasonType, CancelRequesterType cancelRequesterType) {
        log.info("결제취소 요청 portonePaymentId={}, 결제금액={}",payment.getPortonePaymentId(), payment.getAmount());
        PortOneCancelReqDto cancelReqDto = new PortOneCancelReqDto(
                cancelReasonType.getDescription(), cancelRequesterType);
        PortOneCancelResDto cancelResDto = portOneClient.cancel(payment.getPortonePaymentId(), cancelReqDto);

        payment.cancel(
                cancelRequesterType,
                cancelResDto.getTotalAmount(),
                cancelResDto.getReason(),
                cancelResDto.getCancelledDate());

        return cancelResDto;
    }
}
