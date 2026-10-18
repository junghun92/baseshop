package com.baseshop.portone;

import com.baseshop.portone.dto.PortOneCancelReqDto;
import com.baseshop.portone.dto.PortOneCancelResDto;
import com.baseshop.portone.dto.PortOnePaymentResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PortOneClient {

    private final RestClient portOneRestClient;

    /**
     * - 결제 단건 조회
     * 인증 결제(결제창을 이용한 결제)를 위한 엔드포인트입니다.
     *
     * 브라우저에서 결제 완료 후 서버에 결제 완료를 알리는 용도입니다.
     * 결제 수단 및 PG사 사정에 따라 결제 완료 후 승인이 지연될 수 있으므로
     * 결제 정보를 완전히 실시간으로 얻기 위해서는 웹훅을 사용해야 합니다.
     *
     * 인증 결제 연동 가이드: https://developers.portone.io/docs/ko/authpay/guide?v=v2
     */
    public PortOnePaymentResDto getPayment(String portonePaymentId) {
        return portOneRestClient
                .get()
                .uri("/payments/{paymentId}", portonePaymentId)
                .retrieve()
                .body(PortOnePaymentResDto.class);
    }

    /**
     * 결제취소
     */
    public PortOneCancelResDto cancel(String portonePaymentId, PortOneCancelReqDto portOneCancelReqDto) {
        return portOneRestClient
                .post()
                .uri("/payments/{paymentId}/cancel", portonePaymentId)
                .body(portOneCancelReqDto) //요청 바디
                .retrieve()
                .body(PortOneCancelResDto.class);
    }
}

