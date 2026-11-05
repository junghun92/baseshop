package com.baseshop.product.controller;

import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.excel.ExcelUtils;
import com.baseshop.common.handler.ApiResponse;
import com.baseshop.product.dto.*;
import com.baseshop.product.service.ProductExcelService;
import com.baseshop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product")
@Tag(name = "상품 API")
public class ProductController {

    private final ProductService productService;
    private final ProductExcelService productExcelService;

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

    @PutMapping("/{id}/quantity")
    @Operation(summary = "상품 재고 조정 (ADMIN)")
    public ApiResponse<Long> updateQuantity(
            @PathVariable Long id,
            @Valid @RequestBody QuantityUpdateReqDto quantityUpdateReqDto){

        return ApiResponse.success(productService.updateQuantity(id, quantityUpdateReqDto));
    }

    /** 엑셀 다운로드 */
    @GetMapping("/excel")
    public ResponseEntity<StreamingResponseBody> downloadProductExcel(HttpServletResponse response) {
        return ExcelUtils.toResponse("상품목록", productExcelService.createProductExcel());
    }





}
