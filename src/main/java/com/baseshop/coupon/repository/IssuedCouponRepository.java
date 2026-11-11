package com.baseshop.coupon.repository;

import com.baseshop.coupon.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

    /** 쿠폰 리스트 (주어진 쿠폰들 중 이 회원이 받은 쿠폰 ID) */
    @Query("""
            select ic.coupon.id from IssuedCoupon ic
            where ic.member.id = :memberId and ic.coupon.id in :couponIdList
            """)
    List<Long> findIssuedCouponIdList(@Param("memberId") Long memberId,
                                      @Param("couponIdList") List<Long> couponIdList);

    List<IssuedCoupon> findByMemberIdOrderByIdDesc(Long memberId);

}
