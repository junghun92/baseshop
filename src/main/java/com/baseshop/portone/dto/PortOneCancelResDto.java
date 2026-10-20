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

    private Cancellation cancellation;   // 포트원 응답은 cancellation 안에 담겨 온다

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cancellation {

        private static final ZoneId KST = ZoneId.of("Asia/Seoul");

        private String id;                   // 취소 건 아이디
        private CancelStatusType status;
        private long totalAmount;            // 취소 금액
        private String reason;               // 취소 사유
        private OffsetDateTime cancelledAt;  // 취소 완료 시점 (SUCCEEDED일 때)

        public boolean isSucceeded() {
            return "SUCCEEDED".equals(status);
        }

        public boolean isRequested() {
            return "REQUESTED".equals(status);
        }

        public LocalDateTime getCancelledDate() {
            return cancelledAt == null ? null : cancelledAt.atZoneSameInstant(KST).toLocalDateTime();
        }
    }
}
