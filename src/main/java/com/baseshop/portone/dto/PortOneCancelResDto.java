package com.baseshop.portone.dto;

import com.baseshop.portone.type.CancelStatusType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // 필요한 필드만 받고 나머지 무시
public class PortOneCancelResDto {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private String id;               // 결제 건 아이디 (portonePaymentId)
    private CancelStatusType status;
    private long totalAmount; // 취소금액
    private String reason; // 취소 사유
    private OffsetDateTime cancelledAt; // 취소 시점

    public LocalDateTime getCancelledDate() {
        return toKst(cancelledAt);
    }

    private static LocalDateTime toKst(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZoneSameInstant(KST).toLocalDateTime();
    }
}
