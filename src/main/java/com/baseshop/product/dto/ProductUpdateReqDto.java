package com.baseshop.product.dto;

import com.baseshop.product.type.ProductStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductUpdateReqDto {

    @NotBlank(message = "상품명을 입력해 주세요.")
    @Schema(description = "상품명", example = "테스트 상품1")
    private String name;

    @NotBlank(message = "상품 설명을 입력해 주세요.")
    @Schema(description = "상품설명", example = "티셔츠 입니다.")
    private String description;

    @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
    @Schema(description = "상품가격", example = "1000")
    private int price;

    @NotNull(message = "판매 상태를 선택해 주세요.")
    @Schema(description = "상품상태(판매중: SALE, 품절: SOLD_OUT, 판매중지: STOP)", example = "SALE")
    private ProductStatusType productStatusType;
}
