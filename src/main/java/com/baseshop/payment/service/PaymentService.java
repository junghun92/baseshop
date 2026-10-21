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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public PaymentCompleteResDto updateSync(String portonePaymentId, PortOnePaymentResDto portOnePayment) {
        Payment payment = getPaymentForUpdate(portonePaymentId);

        // Payment DB READY -> 결제 반영 (완료 / 실패 / 금액불일치)
        if (payment.getPaymentStatusType() == PaymentStatusType.READY) {
            return processComplete(payment, portOnePayment);
        }

        // Payment DB PAID + 포트원 CANCELLED -> 취소 반영 (콘솔 취소, 최소 후 DB 반영 실패)
        if (payment.getPaymentStatusType() == PaymentStatusType.PAID && portOnePayment.isCancelled()) {
            updateCancelSync(payment, portOnePayment);
        }

        return processComplete(payment, portOnePayment);
    }

    @Transactional(readOnly = true)
    public PaymentCompleteResDto getPaymentResult(Long memberId, String portonePaymentId) {
        Payment payment = paymentRepository.findByPortonePaymentId(portonePaymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보가 없습니다."));

        if (!payment.getOrder().getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("본인의 주문만 결제할 수 있습니다.");
        }
        return PaymentCompleteResDto.from(payment.getOrder(), payment);
    }

    /** 결제 조회 */
    @Transactional(propagation = Propagation.MANDATORY)
    public Payment getPaymentForUpdate(String portonePaymentId) {
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
     * 결제 상태 조회 (웹훅 사전 확인용)
     * - 처리 대상인지(READY, PAID)만 빠르게 확인하는 용도라 락 없이 조회
     * - 우리 DB에 없는 결제면 null → 웹훅에서 무시 (예외를 던지면 포트원이 재전송을 반복함)
     */
    @Transactional(readOnly = true)
    public PaymentStatusType getPaymentStatus(String portonePaymentId) {
        return paymentRepository.findByPortonePaymentId(portonePaymentId)
                .map(Payment::getPaymentStatusType)
                .orElse(null);
    }

    /**
     * 결제취소
     * - SUCCEEDED만 DB에 반영한다. FAILED, REQUESTED는 예외로 롤백한다.
     *   REQUESTED의 최종 결과는 웹훅으로 반영한다.
     */
    public void cancel(Payment payment, CancelReasonType cancelReasonType, CancelRequesterType cancelRequesterType) {
        log.info("결제취소 요청 portonePaymentId={}, 결제금액={}",payment.getPortonePaymentId(), payment.getAmount());
        PortOneCancelReqDto cancelReqDto = new PortOneCancelReqDto(
                cancelReasonType.getDescription(), cancelRequesterType);
        PortOneCancelResDto.Cancellation cancellation = portOneClient.cancel(payment.getPortonePaymentId(), cancelReqDto);
        payment.cancel(
                cancelRequesterType,
                cancellation.getTotalAmount(),
                cancellation.getReason(),
                cancellation.getCancelledDate());
    }

    /** 포트원에서 취소 반영 (취소는 포트원에서 이미 완료된 상태) */
    private void updateCancelSync(Payment payment, PortOnePaymentResDto portOnePayment) {
        Order order = payment.getOrder();
        payment.cancel(
                CancelRequesterType.ADMIN,
                payment.getAmount(),
                CancelReasonType.EXTERNAL_SYNC.getDescription(),
                portOnePayment.getCancelledDate());
        order.cancel();
        productService.updateStockRestore(order.getOrderItemList());
    }

    /**
     * 만료 대상 결제 ID 조회
     * - READY + 생성 후 threshold 지남, 오래된 순으로 최대 100건
     */
    @Transactional(readOnly = true)
    public List<String> getExpiredPaymentIds(LocalDateTime threshold) {
        return paymentRepository.findExpiredPortonePaymentIds(
                PaymentStatusType.READY, threshold, PageRequest.of(0, 100));
    }

    /**
     * 결제 만료 처리 (결제창만 열고 결제 안 함)
     * - 결제 행 락 → READY 재확인 (그 사이 /complete, 웹훅이 처리했으면 종료)
     * - 결제 FAILED, 주문 FAILED, 재고 복구
     */
    @Transactional
    public void updateExpire(String portonePaymentId) {
        Payment payment = getPaymentForUpdate(portonePaymentId);

        if (payment.getPaymentStatusType() != PaymentStatusType.READY) {
            return;
        }

        Order order = payment.getOrder();
        payment.fail("결제시간 만료 (30분)", LocalDateTime.now()); // 결제실패 (결제시간 만료)
        order.fail(); // 주문실패
        productService.updateStockRestore(order.getOrderItemList()); //재고 복구
        log.info("결제 만료 처리 portonePaymentId={}", portonePaymentId);
    }

    /** 결제 준비(READY) 생성 */
    @Transactional
    public Payment createReady(Order order) {
        return paymentRepository.save(Payment.createReady(order));
    }

    /**
     * 취소 가능한 결제 확인 (락 없이 사전확인)
     * - 본인 주문 + 결제완료(PAID)만 취소 가능
     */
    @Transactional(readOnly = true)
    public String getCancelablePaymentId(Long memberId, Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 주문내역이 없습니다."));

        if (!payment.getOrder().getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("본인의 주문만 취소할 수 있습니다.");
        }

        if (payment.getPaymentStatusType() != PaymentStatusType.PAID) {
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        return payment.getPortonePaymentId();
    }
}
