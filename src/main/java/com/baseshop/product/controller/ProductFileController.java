package com.baseshop.product.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.product.dto.ProductFileResDto;
import com.baseshop.product.service.ProductFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product/{id}/file")
public class ProductFileController {

    private final ProductFileService productFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductFileResDto> createProductFile(@PathVariable Long id, @RequestParam("file") MultipartFile multipartFile){
        return ApiResponse.success(productFileService.createProductFile(id, multipartFile));
    }
}
