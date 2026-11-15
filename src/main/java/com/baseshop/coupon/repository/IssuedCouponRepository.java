package com.baseshop.coupon.repository;

import com.baseshop.coupon.entity.IssuedCoupon;
import com.baseshop.coupon.type.IssuedCouponStatusType;
import com.baseshop.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    Optional<IssuedCoupon> findByIdAndMemberId(Long id, Long memberId);

    /**
     * 주문 : 쿠폰 사용 (ISSUED 일 때만 USED 로)
     * - 바뀐 행 수 반환 : 0 이면 이미 사용된 쿠폰 (같은 쿠폰 동시 사용 방지)
     */
    @Modifying
    @Query("""
            update IssuedCoupon ic
            set ic.issuedCouponStatusType = :usedStatus, ic.order = :order, ic.usedDate = :now
            where ic.id = :id and ic.issuedCouponStatusType = :issuedStatus
            """)
    int updateIssuedCouponUse(@Param("id") Long id,
                              @Param("order") Order order,
                              @Param("now") LocalDateTime now,
                              @Param("usedStatus") IssuedCouponStatusType usedStatus,
                              @Param("issuedStatus") IssuedCouponStatusType issuedStatus);

    /** 결제 실패,취소 : 이 주문에 사용한 쿠폰 */
    List<IssuedCoupon> findAllByOrderId(Long orderId);

}
