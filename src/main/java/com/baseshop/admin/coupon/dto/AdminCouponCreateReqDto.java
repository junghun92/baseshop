package com.baseshop.admin.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AdminCouponCreateReqDto {

    @Schema(description = "쿠폰 이름", example = "오픈 기념 3,000원 할인")
    @NotBlank(message = "쿠폰 이름은 필수입니다.")
    @Size(max = 100, message = "쿠폰 이름은 100자 이하입니다.")
    private String name;

    @Schema(description = "할인 금액 (정액)", example = "3000")
    @Positive(message = "할인 금액은 0원보다 커야 합니다.")
    private int discountAmount;

    @Schema(description = "최소 주문 금액", example = "20000")
    @PositiveOrZero(message = "최소 주문 금액은 0원 이상이어야 합니다.")
    private int minOrderAmount;

    @Schema(description = "총 발급 수량", example = "100")
    @Positive(message = "발급 수량은 1장 이상이어야 합니다.")
    private int totalQuantity;

    @Schema(description = "발급 시작일시", example = "2026-11-20T00:00:00")
    @NotNull(message = "발급 시작일은 필수입니다.")
    private LocalDateTime issueStartDate;

    @Schema(description = "발급 종료일시", example = "2026-11-30T23:59:59")
    @NotNull(message = "발급 종료일은 필수입니다.")
    private LocalDateTime issueEndDate;

    @Schema(description = "발급일로부터 사용 가능 일수 (없으면 7일)", example = "7", defaultValue = "7")
    @Positive(message = "사용 가능 일수는 1일 이상이어야 합니다.")
    private int validDays = 7;   // 요청에 없으면 7일
}
