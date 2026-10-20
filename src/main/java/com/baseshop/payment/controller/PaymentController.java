package com.baseshop.payment.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.payment.dto.PaymentCompleteReqDto;
import com.baseshop.payment.dto.PaymentCompleteResDto;
import com.baseshop.payment.service.PaymentFacade;
import com.baseshop.portone.PortOneWebhookVerifier;
import com.baseshop.portone.dto.PortOneWebhookReqDto;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentFacade paymentFacade;
    private final PortOneWebhookVerifier portOneWebhookVerifier;

    @PostMapping("/complete")
    @Operation(summary = "결제완료")
    public ApiResponse<PaymentCompleteResDto> complete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentCompleteReqDto paymentCompleteReqDto){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(paymentFacade.complete(memberId, paymentCompleteReqDto));
    }

    @PostMapping("/webhook")
    @Operation(summary = "웹훅")
    public ResponseEntity<Void> handleWebhook(
            // 웹훅 검증 시 텍스트로 된 body가 필요합니다.
            @RequestBody String body,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature){
        PortOneWebhookReqDto webhook = portOneWebhookVerifier.verify(body, webhookId, webhookTimestamp, webhookSignature);

        if(webhook.isPaymentEvent()){
            paymentFacade.updateWebhook(webhook.getData().getPaymentId());
        }
        return ResponseEntity.ok().build();
    }


}
