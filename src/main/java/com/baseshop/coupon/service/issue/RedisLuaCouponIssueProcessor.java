package com.baseshop.coupon.service.issue;

import com.baseshop.common.exception.BusinessException;
import com.baseshop.common.exception.ErrorCode;
import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.entity.IssuedCoupon;
import com.baseshop.coupon.repository.CouponIssuedRedisRepository;
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
public class RedisLuaCouponIssueProcessor implements CouponIssuedProcessor {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final MemberRepository memberRepository;
    private final CouponIssuedRedisRepository couponIssuedRedisRepository;

    @Transactional
    @Override
    public Long createIssuedCoupon(Long id, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 쿠폰 조회
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 쿠폰 발급가능 여부 확인 (이미 받았는지는 Redis가 판단)
        validateIssue(coupon.checkIssue(false, now));

        // 쿠폰 재고 차감 + 발급된 사용자 확인
        couponIssuedRedisRepository.tryIssue(id, memberId);

        // 쿠폰 발급 수량 +1
        couponRepository.incremantIssuedQuantity(id);

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
