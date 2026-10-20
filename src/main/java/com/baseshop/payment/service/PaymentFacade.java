package com.baseshop.payment.service;

import com.baseshop.payment.dto.PaymentCompleteReqDto;
import com.baseshop.payment.dto.PaymentCompleteResDto;
import com.baseshop.payment.type.PaymentStatusType;
import com.baseshop.portone.PortOneClient;
import com.baseshop.portone.dto.PortOnePaymentResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private static final long EXPIRE_MINUTES = 30;   // 결제 대기 만료 시간

    private final PaymentService paymentService;
    private final PortOneClient portOneClient;

    /**
     * 결제완료 (프론트 -> /complete)
     */
    public PaymentCompleteResDto complete(Long memberId, PaymentCompleteReqDto paymentCompleteReqDto) {
        String portonePaymentId = paymentCompleteReqDto.getPortonePaymentId();

        // 1. 본인 확인 + 처리된 결제는 그대로 반환
        PaymentCompleteResDto paymentResult = paymentService.getPaymentResult(memberId, portonePaymentId);
        if (!paymentResult.isReady()) {
            return paymentResult;
        }

        // 2. 포트원 조회 (트랜잭션 밖)
        PortOnePaymentResDto portOnePayment = portOneClient.getPayment(portonePaymentId);

        // 3. 동기화 (락 + 상태 재확인)
        return paymentService.updateSync(portonePaymentId, portOnePayment);
    }

    /**
     * 웹훅 (포트원 -> 서버)
     */
    public void updateWebhook(String paymentId) {
        // 1. 처리 대상인지 확인
        PaymentStatusType paymentStatus = paymentService.getPaymentStatus(paymentId);
        if (paymentStatus != PaymentStatusType.READY && paymentStatus != PaymentStatusType.PAID) {
            return;
        }

        // 2. 포트원 조회 (트랜잭션 밖)
        PortOnePaymentResDto portOnePayment = portOneClient.getPayment(paymentId);

        // 3. 동기화 (락 + 상태 재확인)
        paymentService.updateSync(paymentId, portOnePayment);
    }

    /**
     * 결제 안 한 주문 정리 (스케줄러에서 호출)
     * - 30분 지난 READY 결제 중 포트원에 결제 시도가 없는 건만 만료 처리 (재고 복구)
     * - 포트원에 결제 결과가 있는 건은 건너뜀 -> 웹훅(재전송 포함)이 처리
     */
    public void updateExpiredPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(EXPIRE_MINUTES);
        List<String> portonePaymentIds = paymentService.getExpiredPaymentIds(threshold);

        for (String portonePaymentId : portonePaymentIds) {
            try {
                if (hasPaymentResult(portonePaymentId)) {
                    log.warn("만료 대상 (포트원 결제 결과 있음) -> 웹훅에서 처리 예정 portonePaymentId={}", portonePaymentId);
                    continue;
                }
                paymentService.updateExpire(portonePaymentId); // 결제 FAILED, 주문 FAILED, 재고 복구
            } catch (Exception e) {
                log.error("결제 만료 처리 실패 portonePaymentId={}", portonePaymentId, e);
            }
        }
    }

    /**
     * 포트원에 결제 결과(완료, 실패, 취소)가 있는지 확인
     */
    private boolean hasPaymentResult(String portonePaymentId) {
        try {
            PortOnePaymentResDto portOnePayment = portOneClient.getPayment(portonePaymentId);
            return portOnePayment.isPaid() || portOnePayment.isFailed() || portOnePayment.isCancelled();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
