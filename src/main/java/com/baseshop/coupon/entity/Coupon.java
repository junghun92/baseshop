package com.baseshop.coupon.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.coupon.type.CouponStatusType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int discountAmount;

    @Column(nullable = false)
    private int minOrderAmount;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private LocalDateTime issueStartDate;

    @Column(nullable = false)
    private LocalDateTime issueEndDate;

    @Column(nullable = false)
    private int validDays;

    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private CouponStatusType couponStatusType;

    @Builder
    private Coupon(String name, int discountAmount, int minOrderAmount, int totalQuantity,
                  LocalDateTime issueStartDate, LocalDateTime issueEndDate, int validDays) {
        this.name = name;
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;
        this.validDays = validDays;
        this.couponStatusType = CouponStatusType.ACTIVE;
    }

    /** 쿠폰상태: 발급가능, 발급 시작 전*/
    private boolean isUpdatable(LocalDateTime now) {
        return couponStatusType == CouponStatusType.ACTIVE && now.isBefore(issueStartDate);
    }

    /** 쿠폰 수정 */
    public void changeCoupon(String name, int discountAmount, int minOrderAmount, int totalQuantity,
                             LocalDateTime issueStartDate, LocalDateTime issueEndDate, int validDays, LocalDateTime now) {

        if (!isUpdatable(now)) {
            throw new IllegalArgumentException("발급이 시작되었거나 중지된 쿠폰은 수정할 수 없습니다.");
        }

        this.name = name;
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
        this.totalQuantity = totalQuantity;
        this.issueStartDate = issueStartDate;
        this.issueEndDate = issueEndDate;
        this.validDays = validDays;
    }

    /** 쿠폰 발급 중지 */
    public void stop() {
        this.couponStatusType = CouponStatusType.STOPPED;
    }
}
