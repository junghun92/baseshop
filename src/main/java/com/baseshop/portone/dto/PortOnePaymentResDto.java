package com.baseshop.portone.dto;

import com.baseshop.portone.type.PortOnePaymentStatusType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // 필요한 필드만 받고 나머지 무시
public class PortOnePaymentResDto {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private String id;               // 결제 건 아이디 (portonePaymentId)
    private PortOnePaymentStatusType status;
    private String transactionId;    // 결제 건 포트원 채번 아이디
    private String currency;         // KRW
    private Amount amount;           // 결제 금액 세부 정보
    private Method method;           // 결제수단 정보 (없을 수 있음)
    private OffsetDateTime paidAt;   // 결제 완료 시점
    private OffsetDateTime failedAt; // 결제 실패 시점
    private Failure failure;         // 결제 실패 정보

    public LocalDateTime getPaidDate() {
        return toKst(paidAt);
    }

    public LocalDateTime getFailedDate() {
        return toKst(failedAt);
    }

    private static LocalDateTime toKst(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZoneSameInstant(KST).toLocalDateTime();
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private long total;          // 총 결제금액
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Method {
        private String type;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Failure {
        private String reason; // 실패사유
    }
}
