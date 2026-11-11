package com.baseshop.coupon.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.coupon.dto.IssuedCouponListResDto;
import com.baseshop.coupon.service.IssuedCouponService;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/issued-coupon")
@Tag(name = "발급 쿠폰 API")
public class IssuedCouponController {

    private final IssuedCouponService issuedCouponService;

    @GetMapping
    @Operation(summary = "발급 쿠폰 리스트")
    public ApiResponse<List<IssuedCouponListResDto>> getIssuedCouponList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        return ApiResponse.success(issuedCouponService.getIssuedCouponList(memberId));
    }

}
