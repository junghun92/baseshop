package com.baseshop.order.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.member.entity.Member;
import com.baseshop.order.type.OrderStatusType;
import com.baseshop.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
    private String orderName;

    @Column(nullable = false)
    private long totalPrice;

    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private OrderStatusType orderStatusType;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItemList = new ArrayList<>();

    public static Order createPending(Member member) {
        Order order = new Order();
        order.member = member;
        order.totalPrice = 0L;
        order.orderName = "";
        order.orderStatusType = OrderStatusType.PENDING;
        return order;
    }

    // 주문상품 추가: 생성 + 양방향 연결 + 총액 누적을 한 번에
    public OrderItem addOrderItem(Product product, int quantity) {
        OrderItem orderItem = new OrderItem(this, product, quantity);   // 같은 패키지라 접근 가능
        orderItemList.add(orderItem);
        totalPrice += orderItem.getTotalPrice();
        updateOrderName();
        return orderItem;
    }

    // 상품 1건: "상품명", 2건 이상: "첫 상품명 외 N건"
    private void updateOrderName() {
        String firstProductName = orderItemList.get(0).getProduct().getName();
        int otherCount = orderItemList.size() - 1;

        this.orderName = otherCount == 0
                ? firstProductName
                : firstProductName + " 외 " + otherCount + "건";
    }

    // 주문완료
    public void complete() {
        this.orderStatusType = OrderStatusType.COMPLETED;
    }

    public void fail(){
        this.orderStatusType = OrderStatusType.FAILED;
    }

    // 주문취소
    public void cancel() {
        if(this.orderStatusType != OrderStatusType.COMPLETED){
            throw new IllegalArgumentException("취소할 수 없는 주문입니다.");
        }

        this.orderStatusType = OrderStatusType.CANCELLED;
    }
}
