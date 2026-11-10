package com.baseshop.product.controller;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.handler.ApiResponse;
import com.baseshop.product.dto.ProductDetailResDto;
import com.baseshop.product.dto.ProductListReqDto;
import com.baseshop.product.dto.ProductListResDto;
import com.baseshop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product")
@Tag(name = "상품 API")
public class ProductController {

    private final ProductService productService;

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
