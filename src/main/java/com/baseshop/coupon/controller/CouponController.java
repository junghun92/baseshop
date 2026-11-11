package com.baseshop.coupon.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.coupon.dto.CouponListResDto;
import com.baseshop.coupon.service.CouponService;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupon")
@Tag(name = "쿠폰 API")
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    @Operation(summary = "쿠폰 리스트")
    public ApiResponse<List<CouponListResDto>> getCouponList(@AuthenticationPrincipal CustomUserDetails userDetails){
        Long memberId = userDetails.getMemberId();
        return ApiResponse.success(couponService.getCouponList(memberId));
    }

    @PostMapping("{id}/issue")
    @Operation(summary = "쿠폰 발급")
    public ApiResponse<Long> issueCoupon(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails){
        Long memberId = userDetails.getMemberId();
        return ApiResponse.success(couponService.createIssuedCoupon(id, memberId));
    }

}
