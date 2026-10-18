package com.baseshop.product.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.product.dto.ProductUpdateReqDto;
import com.baseshop.product.type.ProductStatusType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private ProductStatusType productStatusType;

    @Builder
    public Product(String name, String description, int price, int quantity, ProductStatusType productStatusType) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.productStatusType = productStatusType;
    }

    public void changeProduct(ProductUpdateReqDto productUpdateReqDto) {
        this.name = productUpdateReqDto.getName();
        this.description = productUpdateReqDto.getDescription();
        this.price = productUpdateReqDto.getPrice();
        this.productStatusType = productUpdateReqDto.getProductStatusType();
    }

    public void changeStatus(ProductStatusType productStatusType) {
        this.productStatusType = productStatusType;
    }

    // 주문 -> 상품 재고차감
    public void removeQuantity(int quantity) {
        if(quantity < 1){
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }

        validateOnSale();

        validateQuantity(quantity);
        this.quantity -= quantity;
    }

    public void validateQuantity(int quantity) {
        if (this.quantity < quantity) {
            String str = String.format("재고가 부족합니다. (상품명: %s, 현재재고: %d, 주문수량: %d)", this.name, this.quantity, quantity);
            throw new IllegalArgumentException(str);
        }
    }

    public void validateOnSale() {
        if(this.productStatusType != ProductStatusType.SALE){
            throw new IllegalArgumentException("판매중인 상품이 아닙니다.");
        }
    }

    // 재고추가
    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    public void changeQuantity(int changeQuantity){
        if (changeQuantity == 0) {
            throw new IllegalArgumentException("변경 수량은 0이 될 수 없습니다. (입고: 양수, 차감: 음수)");
        }

        if (this.quantity + changeQuantity < 0) {
            String str = String.format("재고는 0개 미만이 될 수 없습니다. (상품명: %s, 현재재고: %d, 변경수량: %d)", this.name, this.quantity, changeQuantity);
            throw new IllegalArgumentException(str);
        }

        this.quantity += changeQuantity;
    }

}
