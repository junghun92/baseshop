package com.baseshop.portone;

import com.baseshop.portone.dto.PortOneCancelReqDto;
import com.baseshop.portone.dto.PortOneCancelResDto;
import com.baseshop.portone.dto.PortOnePaymentResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
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
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new IllegalArgumentException("결제가 완료되지 않았습니다.");
                })
                .body(PortOnePaymentResDto.class);
    }

    /**
     * 결제취소
     */
    public PortOneCancelResDto.Cancellation cancel(String portonePaymentId, PortOneCancelReqDto portOneCancelReqDto) {
        PortOneCancelResDto portOneCancelResDto = portOneRestClient
                .post()
                .uri("/payments/{paymentId}/cancel", portonePaymentId)
                .body(portOneCancelReqDto) //요청 바디
                .retrieve()
                .body(PortOneCancelResDto.class);

        if(portOneCancelResDto == null || portOneCancelResDto.getCancellation() == null){
            log.warn("결제취소 응답 없음 portonePaymentId={}", portonePaymentId);
            throw new IllegalArgumentException("결제 취소에 실패했습니다. 잠시 후 다시 시도해주세요. 계속 실패하면 고객센터로 문의해주세요.");
        }

        PortOneCancelResDto.Cancellation cancellation = portOneCancelResDto.getCancellation();

        // 취소 처리중 (비동기 환불, 최종 결과는 웹훅으로 반영)
        if(cancellation.isRequested()){
            log.warn("포트원 결제취소 처리중 portonePaymentId={}", portonePaymentId);
            throw new IllegalArgumentException("취소 요청이 접수되었습니다. 환불 완료까지 시간이 걸릴 수 있습니다.");
        }

        // 완료가 아니면 전부 실패
        if(!cancellation.isSucceeded()){
            log.warn("결제취소 실패 portonePaymentId={}", portonePaymentId);
            throw new IllegalArgumentException("결제 취소에 실패했습니다. 잠시 후 다시 시도해주세요. 계속 실패하면 고객센터로 문의해주세요.");
        }

        return cancellation;
    }
}

