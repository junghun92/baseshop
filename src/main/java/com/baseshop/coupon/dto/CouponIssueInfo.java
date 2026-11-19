package com.baseshop.coupon.dto;

import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.type.CouponIssueCheck;
import com.baseshop.coupon.type.CouponStatusType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CouponIssueInfo {

    private Long id;
    private CouponStatusType couponStatusType;
    private LocalDateTime issueStartDate;
    private LocalDateTime issueEndDate;
    private int validDays;

    public static CouponIssueInfo from (Coupon coupon) {
        CouponIssueInfo couponIssueInfo = new CouponIssueInfo();
        couponIssueInfo.id = coupon.getId();
        couponIssueInfo.couponStatusType = coupon.getCouponStatusType();
        couponIssueInfo.issueStartDate = coupon.getIssueStartDate();
        couponIssueInfo.issueEndDate = coupon.getIssueEndDate();
        couponIssueInfo.validDays = coupon.getValidDays();
        return couponIssueInfo;
    }

    /** 발급 확인 (이미 받음,매진은 Redis가 판단) */
    public CouponIssueCheck checkIssue(LocalDateTime now) {
        if (couponStatusType != CouponStatusType.ACTIVE || !now.isBefore(issueEndDate)) {
            return CouponIssueCheck.UNAVAILABLE;
        }
        if (now.isBefore(issueStartDate)) {
            return CouponIssueCheck.UPCOMING;
        }
        return CouponIssueCheck.AVAILABLE;
    }
}
