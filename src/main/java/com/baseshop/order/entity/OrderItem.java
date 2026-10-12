package com.baseshop.order.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int orderPrice;

    @Column(nullable = false)
    private int quantity;

    public static OrderItem createOrderItem(Order savedOrder, Product product, int quantity) {
        // 재고 차감
        product.removeQuantity(quantity);

        OrderItem orderItem = new OrderItem();
        orderItem.order = savedOrder;
        orderItem.product = product;
        orderItem.orderPrice = product.getPrice();
        orderItem.quantity = quantity;
        return orderItem;
    }

    public long getTotalPrice() {
        return (long) this.orderPrice * this.quantity;
    }

}
