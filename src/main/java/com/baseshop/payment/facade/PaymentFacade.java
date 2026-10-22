package com.baseshop.payment.facade;

import com.baseshop.payment.dto.PaymentCompleteReqDto;
import com.baseshop.payment.dto.PaymentCompleteResDto;
import com.baseshop.payment.entity.Payment;
import com.baseshop.payment.service.PaymentService;
import com.baseshop.portone.PortOneClient;
import com.baseshop.portone.dto.PortOnePaymentResDto;
import com.baseshop.portone.exception.PortOnePaymentNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    @Value("${batch.payment-expire-minutes}")
    private long expireMinutes; // 결제 대기 만료 시간

    private final PaymentProcessor paymentProcessor;
    private final PaymentService paymentService;
    private final PortOneClient portOneClient;

    /**
     * 결제완료 (프론트 -> complete)
     */
    public PaymentCompleteResDto complete(Long memberId, PaymentCompleteReqDto paymentCompleteReqDto) {
        String portonePaymentId = paymentCompleteReqDto.getPortonePaymentId();

        // 결제조회, 주문회원 확인
        Payment payment = paymentService.findPayment(portonePaymentId)
                .filter(p -> p.getOrder().isOrderedBy(memberId))
                .orElseThrow(() -> new EntityNotFoundException("결제 정보가 없습니다."));

        return sync(portonePaymentId, payment);
    }

    /**
     * 웹훅 (포트원 -> updateWebhook)
     */
    public void updateWebhook(String paymentId) {
        // 결제 내역이 없으면 무시 (예외를 던지면 포트원이 재전송을 반복함)
        paymentService.findPayment(paymentId)
                .ifPresent(pament -> sync(paymentId, pament));
    }

    /** 동기화 (결제완료, 웹훅 공통) */
    private PaymentCompleteResDto sync(String portonePaymentId, Payment payment) {
        // 1. 처리 대상 확인: READY, PAID만 진행 (FAILED, CANCELLED는 끝난 결제 -> 그대로 반환)
        if (!payment.isReady() && !payment.isPaid()) {
            return PaymentCompleteResDto.of(payment.getOrder(), payment);
        }

        // 2. 포트원 조회
        PortOnePaymentResDto portOnePayment = portOneClient.getPayment(portonePaymentId);

        // 3. 동기화 (락 + 상태 재확인)
        return paymentProcessor.updateSync(portonePaymentId, portOnePayment);
    }

    /** 결제 대기(READY) 10분이 지난 데이터 처리 */
    public void updateExpiredPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(expireMinutes);
        // DB READY + 10분 지난 결제 조회
        List<String> portonePaymentIdList = paymentService.getExpiredPaymentIdList(threshold);

        for (String portonePaymentId : portonePaymentIdList) {
            try {
                // 1. 포트원에 결제 결과 있음 -> 동기화
                PortOnePaymentResDto portOnePayment = portOneClient.getPayment(portonePaymentId);
                if(portOnePayment.hasResult()) { // 성공,취소,실패 값만 처리.
                    log.info("만료 대상 (포트원 결제 결과 있음) 배치에서 sync 처리. portonePaymentId={}", portonePaymentId);
                    paymentProcessor.updateSync(portonePaymentId, portOnePayment);
                }
            } catch (PortOnePaymentNotFoundException e) {
                // 2. 포트원에 결제 결과 없음 → 만료 처리 (결제실패, 주문실패, 재고복구)
                paymentProcessor.updateExpire(portonePaymentId);
            } catch (Exception e){
                log.error("결제 만료 처리 실패 portonePaymentId={}", portonePaymentId, e);
            }
        }
    }
    

}
