package com.baseshop.cart.controller;

import com.baseshop.cart.dto.CartCreateReqDto;
import com.baseshop.cart.dto.CartListReqDto;
import com.baseshop.cart.dto.CartListResDto;
import com.baseshop.cart.dto.CartUpdateReqDto;
import com.baseshop.cart.service.CartService;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.handler.ApiResponse;
import com.baseshop.product.dto.ProductListResDto;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
@Tag(name = "장바구니 API")
public class CartController {

    private final CartService cartService;

    @PostMapping
    @Operation(summary = "장바구니 등록")
    public ApiResponse<Long> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CartCreateReqDto cartCreateReqDto){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(cartService.create(memberId, cartCreateReqDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "장바구니 수정 (수량 변경)")
    public ApiResponse<Long> update(
            @PathVariable Long id,
            @RequestBody CartUpdateReqDto cartUpdateReqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(cartService.update(id, cartUpdateReqDto, memberId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "장바구니 삭제")
    public ApiResponse<Long> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(cartService.delete(id, memberId));
    }

    @GetMapping
    @Operation(summary = "장바구니 리스트")
    public ApiResponse<PagingResDto<CartListResDto>> getProductList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            CartListReqDto cartListReqDto){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(cartService.getCartList(memberId, cartListReqDto));
    }

}
