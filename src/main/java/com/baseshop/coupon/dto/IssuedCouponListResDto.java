package com.baseshop.coupon.dto;

import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.entity.IssuedCoupon;
import com.baseshop.coupon.type.IssuedCouponStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class IssuedCouponListResDto {

    @Schema(description = "받은 쿠폰 번호", example = "10")
    private Long issuedCouponId;

    @Schema(description = "쿠폰 번호", example = "2")
    private Long couponId;

    @Schema(description = "쿠폰 이름", example = "오픈 기념 3,000원 할인")
    private String name;

    @Schema(description = "할인 금액 (정액)", example = "3000")
    private int discountAmount;

    @Schema(description = "최소 주문 금액", example = "20000")
    private int minOrderAmount;

    @Schema(description = "받은 일시", example = "2026-11-20T10:00:00")
    private LocalDateTime issuedDate;

    @Schema(description = "만료 일시", example = "2026-11-27T10:00:00")
    private LocalDateTime expireDate;

    @Schema(description = "쿠폰 상태 (ISSUED: 사용 가능, USED: 사용 완료, EXPIRED: 기간 만료)", example = "ISSUED")
    private IssuedCouponStatusType issuedCouponStatusType;

    @Schema(description = "쿠폰 상태 설명", example = "사용 가능")
    private String issuedCouponStatusDescription;

    public static IssuedCouponListResDto from(IssuedCoupon issuedCoupon) {
        Coupon coupon = issuedCoupon.getCoupon();

        IssuedCouponListResDto dto = new IssuedCouponListResDto();
        dto.issuedCouponId = issuedCoupon.getId();
        dto.couponId = coupon.getId();
        dto.name = coupon.getName();
        dto.discountAmount = coupon.getDiscountAmount();
        dto.minOrderAmount = coupon.getMinOrderAmount();
        dto.issuedDate = issuedCoupon.getRegDate();
        dto.expireDate = issuedCoupon.getExpireDate();
        dto.issuedCouponStatusType = issuedCoupon.getIssuedCouponStatusType();
        dto.issuedCouponStatusDescription = dto.issuedCouponStatusType.getDescription();
        return dto;
    }

}
