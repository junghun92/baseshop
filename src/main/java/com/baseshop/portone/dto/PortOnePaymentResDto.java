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
    private OffsetDateTime cancelledAt;

    public LocalDateTime getPaidDate() {
        return toKst(paidAt);
    }

    public LocalDateTime getFailedDate() {
        return toKst(failedAt);
    }

    public LocalDateTime getCancelledDate() {
        return toKst(cancelledAt);
    }

    public boolean isCancelled() {
        return status == PortOnePaymentStatusType.CANCELLED;
    }
    public boolean isPaid() {
        return status == PortOnePaymentStatusType.PAID;
    }
    public boolean isFailed() {
        return status == PortOnePaymentStatusType.FAILED;
    }


    /** 결제 결과(완료, 실패, 취소)가 나왔는지 */
    public boolean hasResult() {
        return isPaid() || isFailed() || isCancelled();
    }

    private static LocalDateTime toKst(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZoneSameInstant(KST).toLocalDateTime();
    }

    /** 포트원 결제완료인데 주문금액과 결제금액이 다른지 확인 */
    public boolean isAmountMismatch(long orderAmount) {
        return isPaid() && amount.getTotal() != orderAmount;
    }

    /** 결제수단 타입 (결제수단 정보가 없으면 null) */
    public String getMethodType() {
        return method == null ? null : method.getType();
    }

    /** 실패 사유 (실패 정보가 없으면 "결제실패") */
    public String getFailReason() {
        return failure == null ? "결제실패" : failure.getReason();
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
