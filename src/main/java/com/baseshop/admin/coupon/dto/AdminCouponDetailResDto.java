package com.baseshop.admin.coupon.dto;

import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.type.CouponStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminCouponDetailResDto {

    @Schema(description = "쿠폰 번호", example = "1")
    private Long couponId;

    @Schema(description = "쿠폰 이름", example = "오픈 기념 3,000원 할인")
    private String name;

    @Schema(description = "할인 금액 (정액)", example = "3000")
    private int discountAmount;

    @Schema(description = "최소 주문 금액", example = "20000")
    private int minOrderAmount;

    @Schema(description = "총 발급 수량", example = "100")
    private int totalQuantity;

    @Schema(description = "발급된 수량", example = "0")
    private int issuedQuantity;

    @Schema(description = "발급 시작일시", example = "2026-11-20T00:00:00")
    private LocalDateTime issueStartDate;

    @Schema(description = "발급 종료일시", example = "2026-11-30T23:59:59")
    private LocalDateTime issueEndDate;

    @Schema(description = "발급일로부터 사용 가능 일수", example = "7")
    private int validDays;

    @Schema(description = "쿠폰 상태 (ACTIVE: 발급 가능, STOPPED: 발급 중지)", example = "ACTIVE")
    private CouponStatusType couponStatusType;

    @Schema(description = "쿠폰 상태 설명", example = "발급 가능")
    private String couponStatusDescription;

    public static AdminCouponDetailResDto from(Coupon coupon) {
        AdminCouponDetailResDto dto = new AdminCouponDetailResDto();
        dto.couponId = coupon.getId();
        dto.name = coupon.getName();
        dto.discountAmount = coupon.getDiscountAmount();
        dto.minOrderAmount = coupon.getMinOrderAmount();
        dto.totalQuantity = coupon.getTotalQuantity();
        dto.issuedQuantity = coupon.getIssuedQuantity();
        dto.issueStartDate = coupon.getIssueStartDate();
        dto.issueEndDate = coupon.getIssueEndDate();
        dto.validDays = coupon.getValidDays();
        dto.couponStatusType = coupon.getCouponStatusType();
        dto.couponStatusDescription = coupon.getCouponStatusType().getDescription();
        return dto;
    }
}
