package com.baseshop.cart.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.member.entity.Member;
import com.baseshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "product_id"}))
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Builder
    public Cart(Member member, Product product, int quantity) {
        validateQuantity(quantity);
        this.member = member;
        this.product = product;
        this.quantity = quantity;
    }

    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public void addQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity += quantity;
    }

    private void validateQuantity(int quantity){
        if(quantity < 1){
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }
}
