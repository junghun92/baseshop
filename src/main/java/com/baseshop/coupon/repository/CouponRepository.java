package com.baseshop.coupon.repository;

import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.type.CouponStatusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("""
            select c from Coupon c
            where c.couponStatusType = :couponStatusType and c.issueEndDate > :now
            order by c.issueStartDate asc
            """)
    List<Coupon> findCouponList(@Param("couponStatusType") CouponStatusType couponStatusType,
                                @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
}
