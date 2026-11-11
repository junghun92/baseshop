package com.baseshop.coupon.service.issue;

import com.baseshop.common.exception.BusinessException;
import com.baseshop.common.exception.ErrorCode;
import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.entity.IssuedCoupon;
import com.baseshop.coupon.repository.CouponRepository;
import com.baseshop.coupon.repository.IssuedCouponRepository;
import com.baseshop.coupon.type.CouponIssueCheck;
import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PessimisticLockCouponIssueProcessor implements CouponIssuedProcessor {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public Long createIssuedCoupon(Long id, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 쿠폰 조회
        Coupon coupon = couponRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 쿠폰 발급가능 여부 확인
        boolean issued = issuedCouponRepository.existsByMemberIdAndCouponId(memberId, id);
        validateIssue(coupon.checkIssue(issued, now));

        // 쿠폰 발급 수량 +1
        coupon.increaseIssuedQuantity();

        // 쿠폰 발급 저장
        Member member = memberRepository.getReferenceById(memberId);
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .coupon(coupon)
                .member(member)
                .expireDate(now)
                .build();
        return issuedCouponRepository.save(issuedCoupon).getId();
    }

    /** 쿠폰 발급가능 여부 확인 */
    private void validateIssue(CouponIssueCheck couponIssueCheck) {
        switch (couponIssueCheck) {
            case UNAVAILABLE -> throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE);
            case ISSUED -> throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
            case UPCOMING -> throw new BusinessException(ErrorCode.COUPON_NOT_STARTED);
            case SOLD_OUT -> throw new BusinessException(ErrorCode.COUPON_SOLD_OUT);
            case AVAILABLE -> { }
        }
    }
}
