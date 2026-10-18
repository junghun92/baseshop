package com.baseshop.payment.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.payment.dto.PaymentCompleteReqDto;
import com.baseshop.payment.dto.PaymentCompleteResDto;
import com.baseshop.payment.service.PaymentFacade;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFasade;

    @PostMapping("/complete")
    @Operation(summary = "결제완료")
    public ApiResponse<PaymentCompleteResDto> complete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentCompleteReqDto paymentCompleteReqDto){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(paymentFasade.complete(memberId, paymentCompleteReqDto));
    }


}
