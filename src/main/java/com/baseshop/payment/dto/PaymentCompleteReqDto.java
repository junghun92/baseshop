package com.baseshop.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentCompleteReqDto {

    @NotBlank(message = "결제 아이디가 없습니다.")
    private String portonePaymentId;
}
