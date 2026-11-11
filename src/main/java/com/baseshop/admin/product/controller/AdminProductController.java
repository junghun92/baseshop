package com.baseshop.admin.product.controller;

import com.baseshop.admin.product.dto.*;
import com.baseshop.admin.product.service.AdminProductExcelService;
import com.baseshop.admin.product.service.AdminProductService;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.excel.ExcelUtils;
import com.baseshop.common.handler.ApiResponse;
import com.baseshop.product.dto.ProductFileResDto;
import com.baseshop.product.service.ProductFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/product")
@Tag(name = "관리자 상품 API")
public class AdminProductController {

    private final ProductFileService productFileService;
    private final AdminProductService adminProductService;
    private final AdminProductExcelService adminProductExcelService;

    @PostMapping
    @Operation(summary = "상품 등록")
    public ApiResponse<Long> create(@Valid @RequestBody AdminProductCreateReqDto adminProductCreateReqDto){
        return ApiResponse.success(adminProductService.create(adminProductCreateReqDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "상품 수정")
    public ApiResponse<Long> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminProductUpdateReqDto adminProductUpdateReqDto){

        return ApiResponse.success(adminProductService.update(id, adminProductUpdateReqDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제 - 상태값을 판매중지로 변경")
    public ApiResponse<Long> delete(@PathVariable Long id){
        return ApiResponse.success(adminProductService.delete(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 상세")
    public ApiResponse<AdminProductDetailResDto> getProductDetail(@PathVariable Long id){
        return ApiResponse.success(adminProductService.getProductDetail(id));
    }

    @GetMapping
    @Operation(summary = "상품 리스트")
    public ApiResponse<PagingResDto<AdminProductListResDto>> getProductList(AdminProductListReqDto adminProductListReqDto){
        return ApiResponse.success(adminProductService.getProductList(adminProductListReqDto));
    }

    @PutMapping("/{id}/quantity")
    @Operation(summary = "상품 재고 조정")
    public ApiResponse<Long> updateQuantity(
            @PathVariable Long id,
            @Valid @RequestBody QuantityUpdateReqDto quantityUpdateReqDto){

        return ApiResponse.success(adminProductService.updateQuantity(id, quantityUpdateReqDto));
    }

    /** 엑셀 다운로드 */
    @GetMapping("/excel")
    @Operation(summary = "상품 엑셀 다운로드")
    public ResponseEntity<StreamingResponseBody> downloadProductExcel() {
        return ExcelUtils.toResponse("상품목록", adminProductExcelService.createProductExcel());
    }
    /** 이미지 업로드 */
    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "상품 이미지 업로드")
    public ApiResponse<ProductFileResDto> createProductFile(@PathVariable Long id, @RequestParam("file") MultipartFile multipartFile){
        return ApiResponse.success(productFileService.create(id, multipartFile));
    }
}
