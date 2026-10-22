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

    /** 주문상품 추가: 생성 + 양방향 연결 + 총액 누적을 한 번에 */
    public OrderItem addOrderItem(Product product, int quantity) {
        OrderItem orderItem = new OrderItem(this, product, quantity);   // 같은 패키지라 접근 가능
        orderItemList.add(orderItem);
        totalPrice += orderItem.getTotalPrice();
        updateOrderName();
        return orderItem;
    }

    /** 상품 1건: "상품명", 2건 이상: "첫 상품명 외 N건" */
    private void updateOrderName() {
        String firstProductName = orderItemList.get(0).getProduct().getName();
        int otherCount = orderItemList.size() - 1;

        this.orderName = otherCount == 0
                ? firstProductName
                : firstProductName + " 외 " + otherCount + "건";
    }

    /**
     * 주문 상태 변경 (상태 변경은 이 메서드로만)
     * - 전이 규칙(OrderStatusType.canTransitTo)에 맞을 때만 변경
     * - 중복 완료, 중복 취소(재고 중복 복구)를 여기서 막는다.
     */
    public void transitTo(OrderStatusType target) {
        if (!this.orderStatusType.canTransitTo(target)) {
            throw new IllegalArgumentException(
                    "주문 상태를 변경할 수 없습니다. (" + this.orderStatusType.getDescription()
                            + " -> " + target.getDescription() + ")");
        }
        this.orderStatusType = target;
    }


    /** 주문한 상품 ID 목록 */
    public List<Long> getProductIdList() {
        return orderItemList.stream()
                .map(orderItem -> orderItem.getProduct().getId())
                .toList();
    }

    /** 주문한 회원인지 확인 */
    public boolean isOrderedBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }
}
