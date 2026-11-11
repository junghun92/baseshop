package com.baseshop.admin.coupon.dto;

import com.baseshop.common.dto.PagingReqDto;
import com.baseshop.coupon.type.CouponStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminCouponListReqDto extends PagingReqDto {

    @Schema(description = "쿠폰상태(발급 가능: ACTIVE, 발급 중지: STOPPED)", example = "ACTIVE")
    private CouponStatusType couponStatusType;

}
