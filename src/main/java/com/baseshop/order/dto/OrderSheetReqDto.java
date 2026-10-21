package com.baseshop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderSheetReqDto {

    @Schema(description = "주문할 상품 목록")
    @NotEmpty(message = "주문할 상품이 없습니다.")
    @Valid
    private List<OrderSheetItemReqDto> itemList = new ArrayList<>();

    @Data
    public static class OrderSheetItemReqDto {
        @Schema(description = "상품번호")
        @NotNull(message = "상품번호는 필수입니다.")
        private Long productId;

        @Schema(description = "주문 수량")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        private int quantity;
    }

}
