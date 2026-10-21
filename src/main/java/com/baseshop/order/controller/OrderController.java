package com.baseshop.order.controller;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.handler.ApiResponse;
import com.baseshop.order.dto.*;
import com.baseshop.order.facade.OrderFacade;
import com.baseshop.order.service.OrderService;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Tag(name = "주문 API")
public class OrderController {

    private final OrderService orderService;
    private final OrderFacade orderFacade;

    @PostMapping
    @Operation(summary = "주문 등록")
    public ApiResponse<OrderCreateResDto> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderCreateReqDto orderCreateReqDto) {
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(orderFacade.create(memberId, orderCreateReqDto));
    }

    @PostMapping("/sheet")
    @Operation(summary = "주문서")
    public ApiResponse<OrderSheetResDto> getOrderSheet(@Valid @RequestBody OrderSheetReqDto orderSheetReqDto) {
        return ApiResponse.success(orderFacade.getOrderSheet(orderSheetReqDto));
    }

    @GetMapping
    @Operation(summary = "주문 목록")
    public ApiResponse<PagingResDto<OrderListResDto>> getOrderList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            OrderListReqDto orderListReqDto){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(orderService.getOrderList(memberId, orderListReqDto));
    }
    
    @PutMapping("/{id}/cancel")
    @Operation(summary = "주문 취소")
    public ApiResponse<Long> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(orderFacade.cancel(memberId, id));
    }

}
