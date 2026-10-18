package com.baseshop.payment.service;

import com.baseshop.payment.dto.PaymentCompleteReqDto;
import com.baseshop.payment.dto.PaymentCompleteResDto;
import com.baseshop.portone.PortOneClient;
import com.baseshop.portone.dto.PortOnePaymentResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final PortOneClient portOneClient;

    public PaymentCompleteResDto complete(Long memberId, PaymentCompleteReqDto paymentCompleteReqDto) {
        String portonePaymentId = paymentCompleteReqDto.getPortonePaymentId();

        // 처리된 결제는 그대로 반환
        Optional<PaymentCompleteResDto> processed = paymentService.getProcessedPaymentResult(memberId, portonePaymentId);
        if (processed.isPresent()) {
            return processed.get();
        }

        // 포트원 API 조회는 트랜잭션 밖에서 진행. DB 커넥션 점유 X
        PortOnePaymentResDto portOnePayment = portOneClient.getPayment(portonePaymentId);

        return paymentService.complete(portonePaymentId, portOnePayment);
    }
}
