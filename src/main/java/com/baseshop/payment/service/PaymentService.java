package com.baseshop.payment.service;

import com.baseshop.order.entity.Order;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.repository.PaymentRepository;
import com.baseshop.payment.type.PaymentStatusType;
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
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /** 결제 조회 */
    @Transactional(propagation = Propagation.MANDATORY)
    public Payment getPaymentForUpdate(String portonePaymentId) {
        return paymentRepository.findByPortonePaymentIdForUpdate(portonePaymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제정보가 없습니다."));
    }

    /** 결제 조회 */
    @Transactional(readOnly = true)
    public Optional<Payment> findPayment(String portonePaymentId) {
        return paymentRepository.findWithOrderByPortonePaymentId(portonePaymentId);
    }


    /**
     * 만료 대상 결제 ID 조회
     * - READY + 생성 후 threshold 지남, 오래된 순으로 최대 100건
     */
    @Transactional(readOnly = true)
    public List<String> getExpiredPaymentIdList(LocalDateTime threshold) {
        return paymentRepository.findExpiredPortonePaymentIdList(
                PaymentStatusType.READY, threshold, PageRequest.of(0, 100));
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

        if (!payment.getOrder().isOrderedBy(memberId)) {
            throw new AccessDeniedException("본인의 주문만 취소할 수 있습니다.");
        }

        if (!payment.isPaid()) {
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        return payment.getPortonePaymentId();
    }
}
