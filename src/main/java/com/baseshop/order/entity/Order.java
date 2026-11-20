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
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_orders_member_idempotency_key",
        columnNames = {"member_id", "idempotency_key"}
))
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

    @Column(nullable = false)
    private long discountAmount;

    @Column(nullable = false)
    private long paymentAmount;

    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private OrderStatusType orderStatusType;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItemList = new ArrayList<>();

    @Column(length = 64)
    private String idempotencyKey; // 주문 생성 멱등성 키 (같은 회원 + 같은 키 = 같은 주문)

    public static Order createPending(Member member, String idempotencyKey) {
        Order order = new Order();
        order.member = member;
        order.idempotencyKey = idempotencyKey;
        order.totalPrice = 0L;
        order.discountAmount = 0L;
        order.paymentAmount = 0L;
        order.orderName = "";
        order.orderStatusType = OrderStatusType.PENDING;
        return order;
    }

    /** 결제 대기 중인 주문인지 */
    public boolean isPending() {
        return this.orderStatusType == OrderStatusType.PENDING;
    }

    /** 주문상품 추가: 생성 + 양방향 연결 + 총액 누적을 한 번에 */
    public OrderItem addOrderItem(Product product, int quantity) {
        OrderItem orderItem = new OrderItem(this, product, quantity);   // 같은 패키지라 접근 가능
        orderItemList.add(orderItem);
        totalPrice += orderItem.getTotalPrice();
        paymentAmount = totalPrice - discountAmount;
        updateOrderName();
        return orderItem;
    }

    /**
     * 쿠폰 할인 적용 (주문 상품을 다 담은 뒤 호출)
     * - 결제 금액이 0원 이하가 되면 결제할 수 없어서 막는다.
     */
    public void applyDiscount(long discountAmount) {
        if (discountAmount <= 0 || discountAmount >= this.totalPrice) {
            throw new IllegalArgumentException(
                    "할인 금액이 올바르지 않습니다. (할인 " + discountAmount + ", 상품 금액 " + this.totalPrice + ")");
        }
        this.discountAmount = discountAmount;
        this.paymentAmount = this.totalPrice - discountAmount;
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
