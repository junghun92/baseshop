package com.baseshop.product.dto;

import com.baseshop.product.type.ProductStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
public class ProductDetailResDto {

    @Schema(description = "상품번호")
    private Long id;

    @Schema(description = "상품명", example = "테스트 상품1")
    private String name;

    @Schema(description = "상품설명", example = "티셔츠 입니다.")
    private String description;

    @Schema(description = "상품가격")
    private int price;

    @Schema(description = "상품재고")
    private int quantity;

    @Schema(description = "상품상태(판매중: SALE, 품절: SOLD_OUT, 판매중지: STOP)", example = "SALE")
    private ProductStatusType productStatusType;

    @Builder
    public ProductDetailResDto(Long id, String name, String description, int price, int quantity, ProductStatusType productStatusType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.productStatusType = productStatusType;
    }
}
