package com.baseshop.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartUpdateReqDto {

    @Schema(description = "수량", example = "1")
    private int quantity;
}
