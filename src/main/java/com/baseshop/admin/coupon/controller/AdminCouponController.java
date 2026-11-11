package com.baseshop.admin.coupon.controller;

import com.baseshop.admin.coupon.dto.*;
import com.baseshop.admin.coupon.service.AdminCouponService;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.handler.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/coupon")
@Tag(name = "관리자 쿠폰 API")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @PostMapping
    @Operation(summary = "쿠폰생성")
    public ApiResponse<Long> create(@Valid @RequestBody AdminCouponCreateReqDto reqDto){
        return ApiResponse.success(adminCouponService.create(reqDto));
    }
    @PutMapping("{id}")
    @Operation(summary = "쿠폰 수정 (발급 시작 전만 가능")
    public ApiResponse<Long> update(@PathVariable Long id, @Valid @RequestBody AdminCouponUpdateReqDto adminCouponUpdateReqDto){
        return ApiResponse.success(adminCouponService.update(id, adminCouponUpdateReqDto));
    }

    @GetMapping
    @Operation(summary = "쿠폰 리스트")
    public ApiResponse<PagingResDto<AdminCouponListResDto>> getCouponList(AdminCouponListReqDto adminCouponListReqDto){
        return ApiResponse.success(adminCouponService.getCouponList(adminCouponListReqDto));
    }

    @GetMapping("{id}")
    @Operation(summary = "쿠폰 상세")
    public ApiResponse<AdminCouponDetailResDto> getCouponDetail(@PathVariable Long id){
        return ApiResponse.success(adminCouponService.getCouponDetail(id));
    }

    @DeleteMapping("{id}")
    @Operation(summary = "쿠폰 삭제 (발급 중지)")
    public ApiResponse<Long> delete(@PathVariable Long id){
        return ApiResponse.success(adminCouponService.delete(id));
    }

}
