package com.baseshop.product.controller;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.handler.ApiResponse;
import com.baseshop.product.dto.*;
import com.baseshop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product")
@Tag(name = "상품 API")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "상품 등록 (ADMIN)")
    public ApiResponse<Long> create(@Valid @RequestBody ProductCreateReqDto productCreateReqDto){
        return ApiResponse.success(productService.create(productCreateReqDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "상품 수정 (ADMIN)")
    public ApiResponse<Long> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateReqDto productUpdateReqDto){

        return ApiResponse.success(productService.update(id, productUpdateReqDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제 - 상태값을 판매중지로 변경 (ADMIN)")
    public ApiResponse<Long> delete(@PathVariable Long id){
        return ApiResponse.success(productService.delete(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 상세")
    public ApiResponse<ProductDetailResDto> getProductDetail(@PathVariable Long id){
        return ApiResponse.success(productService.getProductDetail(id));
    }

    @GetMapping
    @Operation(summary = "상품 리스트")
    public ApiResponse<PagingResDto<ProductListResDto>> getProductList(ProductListReqDto productListReqDto){
        return ApiResponse.success(productService.getProductList(productListReqDto));
    }

}
