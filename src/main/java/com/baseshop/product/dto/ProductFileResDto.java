package com.baseshop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class ProductFileResDto {

    @Schema(description = "파일명")
    private final String originalFileName;

    @Schema(description = "이미지 경로")
    private final String imageUrl;

    public ProductFileResDto(String originalFileName, String imageUrl) {
        this.originalFileName = originalFileName;
        this.imageUrl = imageUrl;
    }
}
