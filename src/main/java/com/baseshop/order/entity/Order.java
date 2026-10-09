package com.baseshop.order.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.member.entity.Member;
import com.baseshop.order.type.OrderStatusType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private long totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatusType orderStatusType;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItemList = new ArrayList<>();

    @Builder
    public Order(Member member, Long totalPrice, OrderStatusType orderStatusType) {
        this.member = member;
        this.totalPrice = totalPrice;
        this.orderStatusType = orderStatusType;
    }

    public void changeTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    // 주문취소
    public void cancel() {
        if(this.orderStatusType != OrderStatusType.ORDER){
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        this.orderStatusType = OrderStatusType.CANCEL;

        for (OrderItem orderItem : this.orderItemList) {
            orderItem.cancel(); //재고 복구
        }
    }
}
