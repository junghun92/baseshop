package com.baseshop.coupon.service;

import com.baseshop.coupon.dto.CouponListResDto;
import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.repository.CouponRepository;
import com.baseshop.coupon.repository.IssuedCouponRepository;
import com.baseshop.coupon.service.issue.CouponIssuedProcessor;
import com.baseshop.coupon.type.CouponStatusType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponIssuedProcessor couponIssuedProcessor;

    public CouponService(CouponRepository couponRepository,
                         IssuedCouponRepository issuedCouponRepository,
                         @Qualifier("redisLuaCouponIssueProcessor") CouponIssuedProcessor couponIssuedProcessor) {
        this.couponRepository = couponRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.couponIssuedProcessor = couponIssuedProcessor;
    }

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
    public Long createIssuedCoupon(Long id, Long memberId) {
        return couponIssuedProcessor.createIssuedCoupon(id, memberId);
    }

}
