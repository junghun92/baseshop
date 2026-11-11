package com.baseshop.coupon.service;

import com.baseshop.common.exception.BusinessException;
import com.baseshop.common.exception.ErrorCode;
import com.baseshop.coupon.dto.CouponListResDto;
import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.entity.IssuedCoupon;
import com.baseshop.coupon.repository.CouponRepository;
import com.baseshop.coupon.repository.IssuedCouponRepository;
import com.baseshop.coupon.type.CouponIssueCheck;
import com.baseshop.coupon.type.CouponStatusType;
import com.baseshop.member.entity.Member;
import com.baseshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<CouponListResDto> getCouponList(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 쿠폰 리스트
        List<Coupon> couponList = couponRepository.findCouponList(CouponStatusType.ACTIVE, now);

        if(couponList.isEmpty()){
            return List.of();
        }

        // 회원 쿠폰별 발급 수 조회
        List<Long> couponIdList = couponList.stream().map(Coupon::getId).toList();
        Set<Long> issuedCouponIdSet = new HashSet<>(issuedCouponRepository.findIssuedCouponIdList(memberId, couponIdList));

        return couponList.stream()
                .map(coupon -> CouponListResDto.of(coupon, issuedCouponIdSet.contains(coupon.getId()), now))
                .toList();
    }

    @Transactional
    public Long issueCoupon(Long id, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 쿠폰 조회
        Coupon coupon = couponRepository.findById(id)
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
