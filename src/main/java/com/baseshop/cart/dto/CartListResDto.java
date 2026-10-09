package com.baseshop.cart.dto;

import com.baseshop.cart.entity.Cart;
import com.baseshop.product.entity.Product;
import com.baseshop.product.type.ProductStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CartListResDto {

    @Schema(description = "장바구니 번호")
    private Long id;

    @Schema(description = "상품 번호")
    private Long productId;

    @Schema(description = "상품명")
    private String productName;

    @Schema(description = "상품 가격 (현재 가격)")
    private int productPrice;

    @Schema(description = "상품 상태 (SALE: 판매중, SOLD_OUT: 품절, STOP: 판매중지)")
    private ProductStatusType productStatusType;

    @Schema(description = "상품 상태 (SALE: 판매중, SOLD_OUT: 품절, STOP: 판매중지)")
    private int productQuantity;

    @Schema(description = "담은 수량")
    private int quantity;

    @Schema(description = "합계 (가격 × 수량)")
    private int totalPrice;

    public CartListResDto(Cart cart) {
        Product product = cart.getProduct(); // fetchJoin

        this.id = cart.getId();
        this.productId = product.getId();
        this.productName = product.getName();
        this.productPrice = product.getPrice();
        this.productStatusType = product.getProductStatusType();
        this.productQuantity = product.getQuantity();
        this.quantity = cart.getQuantity();
        this.totalPrice = product.getPrice() * cart.getQuantity();
    }
}
