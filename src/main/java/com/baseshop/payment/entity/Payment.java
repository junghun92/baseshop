package com.baseshop.payment.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.order.entity.Order;
import com.baseshop.payment.type.PaymentStatusType;
import com.baseshop.portone.type.CancelRequesterType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, unique = true)
    private String portonePaymentId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private PaymentStatusType paymentStatusType;

    @Column
    private String transactionId; //결제 건 포트원 채번 아이디

    @Column(columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private PayMethodType payMethodType;

    private LocalDateTime paidDate; //결제완료 시점

    private String failReason; //실패사유

    private LocalDateTime failedDate; //결제실패 시점

    @Column(columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private CancelRequesterType cancelRequester; //취소 주체

    private long cancelAmount; //취소금액

    private String cancelReason; //취소사유

    private LocalDateTime cancelledDate; //결제취소 시점

    private Payment (Order order) {
        this.order = order;
        this.portonePaymentId = "pay-" + UUID.randomUUID();
        this.amount = order.getTotalPrice();
        this.paymentStatusType = PaymentStatusType.READY;
    }

    public static Payment createReady(Order order){
        return new Payment(order);
    }

    /** 결제완료 */
    public void complete(PayMethodType payMethodType, String transactionId, LocalDateTime paidDate) {
        transitTo(PaymentStatusType.PAID);
        this.payMethodType = payMethodType;
        this.transactionId = transactionId;
        this.paidDate = paidDate;
    }

    /** 결제실패 */
    public void fail(String failReason, LocalDateTime failedDate) {
        transitTo(PaymentStatusType.FAILED);
        this.failReason = failReason;
        this.failedDate = failedDate;
    }

    /** 결제취소 */
    public void cancel(CancelRequesterType cancelRequester, long cancelAmount, String cancelReason, LocalDateTime cancelledDate) {
        transitTo(PaymentStatusType.CANCELLED);
        this.cancelRequester = cancelRequester;
        this.cancelAmount = cancelAmount;
        this.cancelReason = cancelReason;
        this.cancelledDate = cancelledDate;
    }

    /**
     * 결제 상태 변경 (상태 변경은 이 메서드로만)
     * - 전이 규칙(PaymentStatusType.canTransitTo)에 맞을 때만 변경
     */
    private void transitTo(PaymentStatusType target) {
        if (!this.paymentStatusType.canTransitTo(target)) {
            throw new IllegalArgumentException(
                    "결제 상태를 변경할 수 없습니다. (" + this.paymentStatusType.getDescription()
                            + " -> " + target.getDescription() + ")");
        }
        this.paymentStatusType = target;
    }

    public boolean isReady() {
        return this.paymentStatusType == PaymentStatusType.READY;
    }

    public boolean isPaid() {
        return this.paymentStatusType == PaymentStatusType.PAID;
    }

}
