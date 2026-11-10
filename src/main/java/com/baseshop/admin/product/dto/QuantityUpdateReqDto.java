package com.baseshop.admin.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuantityUpdateReqDto {

    @Schema(description = "재고 변경 수량")
    private int changeQuantity;

}
