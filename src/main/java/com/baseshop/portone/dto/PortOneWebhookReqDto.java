package com.baseshop.portone.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortOneWebhookReqDto {

    private String type;
    private WebhookData data;

    @Getter
    @NoArgsConstructor
    public static class WebhookData {
        private String paymentId;
    }

    public boolean isPaymentEvent(){
        boolean paymentType = "Transaction.Paid".equals(type) || "Transaction.Cancelled".equals(type) || "Transaction.Failed".equals(type);
        return paymentType && data != null && data.getPaymentId() != null;
    }
}
