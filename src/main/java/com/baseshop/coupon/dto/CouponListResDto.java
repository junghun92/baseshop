package com.baseshop.coupon.dto;

import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.type.CouponIssueCheck;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponListResDto {

    @Schema(description = "쿠폰 번호", example = "1")
    private Long couponId;

    @Schema(description = "쿠폰 이름", example = "오픈 기념 3,000원 할인")
    private String name;

    @Schema(description = "할인 금액 (정액)", example = "3000")
    private int discountAmount;

    @Schema(description = "최소 주문 금액", example = "20000")
    private int minOrderAmount;

    @Schema(description = "발급 시작일시", example = "2026-11-20T10:00:00")
    private LocalDateTime issueStartDate;

    @Schema(description = "발급 종료일시", example = "2026-11-30T23:59:59")
    private LocalDateTime issueEndDate;

    @Schema(description = "발급일로부터 사용 가능 일수", example = "7")
    private int validDays;

    @Schema(description = "내 쿠폰 상태 (ISSUED: 받음, UPCOMING: 오픈 예정, SOLD_OUT: 매진, AVAILABLE: 발급 가능)", example = "AVAILABLE")
    private CouponIssueCheck couponIssueCheck;

    @Schema(description = "내 쿠폰 상태 설명", example = "발급 가능")
    private String couponIssueCheckDescription;

    public static CouponListResDto of(Coupon coupon, boolean issued, LocalDateTime now) {
        CouponListResDto dto = new CouponListResDto();
        dto.couponId = coupon.getId();
        dto.name = coupon.getName();
        dto.discountAmount = coupon.getDiscountAmount();
        dto.minOrderAmount = coupon.getMinOrderAmount();
        dto.issueStartDate = coupon.getIssueStartDate();
        dto.issueEndDate = coupon.getIssueEndDate();
        dto.validDays = coupon.getValidDays();
        dto.couponIssueCheck = coupon.checkIssue(issued, now);
        dto.couponIssueCheckDescription = dto.couponIssueCheck.getDescription();
        return dto;
    }

}
