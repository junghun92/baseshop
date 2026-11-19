package com.baseshop.coupon.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.common.exception.BusinessException;
import com.baseshop.common.exception.ErrorCode;
import com.baseshop.coupon.type.IssuedCouponStatusType;
import com.baseshop.member.entity.Member;
import com.baseshop.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_issued_coupon_member_coupon", columnNames = {"member_id", "coupon_id"})
)
public class IssuedCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_coupon_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private IssuedCouponStatusType issuedCouponStatusType;

    @Column(nullable = false)
    private LocalDateTime expireDate;

    private LocalDateTime usedDate;

    @Builder
    public IssuedCoupon(Coupon coupon, Member member, LocalDateTime issuedDate, int validDays) {
        this.coupon = coupon;
        this.member = member;
        this.issuedCouponStatusType = IssuedCouponStatusType.ISSUED;
        this.expireDate = issuedDate.plusDays(validDays);
    }

    /**
     * 이 주문에 쓸 수 있는지 확인 (사용 처리 전에 호출)
     * - 실제 사용 처리(USED)는 조건부 UPDATE 로 (같은 쿠폰 동시 사용 방지)
     */
    public void validateUsable(long orderAmount, LocalDateTime now) {
        if (this.issuedCouponStatusType != IssuedCouponStatusType.ISSUED
                || !now.isBefore(this.expireDate)) {
            throw new BusinessException(ErrorCode.ISSUED_COUPON_NOT_USABLE);
        }
        if (orderAmount < this.coupon.getMinOrderAmount()) {
            throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }
    }

    /** 복구 (결제 실패·취소·만료) : USED → ISSUED, 주문 연결 해제 */
    public void restore() {
        if (!this.issuedCouponStatusType.canTransitTo(IssuedCouponStatusType.ISSUED)) {
            throw new IllegalStateException("복구할 수 없는 쿠폰 상태입니다. (" + this.issuedCouponStatusType + ")");
        }
        this.issuedCouponStatusType = IssuedCouponStatusType.ISSUED;
        this.order = null;
        this.usedDate = null;
    }

}
