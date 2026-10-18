package com.baseshop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderSheetResDto {

    @Schema(description = "주문 상품정보 리스트")
    private List<OrderSheetItemResDto> orderItemSheetList = new ArrayList<>();

    @Schema(description = "총 주문금액")
    private long totalAmount;

    public void addItem(OrderSheetItemResDto item) {
        this.orderItemSheetList.add(item);
        this.totalAmount += item.getItemTotalAmount();
    }

    @Data
    public static class OrderSheetItemResDto {
        
        @Schema(description = "상품번호")
        private Long productId;

        @Schema(description = "상품명")
        private String name;

        @Schema(description = "이미지 URL")
        private String imageUrl;

        @Schema(description = "금액")
        private int price;

        @Schema(description = "요청수량")
        private int quantity;

        @Schema(description = "상품별 총 금액 (price * quantity)")
        private int itemTotalAmount;

        @Builder
        public OrderSheetItemResDto(Long productId, String name, String imageUrl, int price, int quantity) {
            this.productId = productId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.price = price;
            this.quantity = quantity;
            this.itemTotalAmount = price * quantity;
        }
    }

}
