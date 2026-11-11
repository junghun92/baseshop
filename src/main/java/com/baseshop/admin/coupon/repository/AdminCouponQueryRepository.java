package com.baseshop.admin.coupon.repository;

import com.baseshop.admin.coupon.dto.AdminCouponListReqDto;
import com.baseshop.coupon.entity.Coupon;
import com.baseshop.coupon.type.CouponStatusType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.baseshop.coupon.entity.QCoupon.coupon;


@Repository
@RequiredArgsConstructor
public class AdminCouponQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Coupon> findCouponList(AdminCouponListReqDto adminCouponListReqDto) {
        PageRequest pageRequest = adminCouponListReqDto.toPageRequest();

        List<Coupon> content = queryFactory
                .selectFrom(coupon)
                .where(getCouponStatusTypeEq(adminCouponListReqDto.getCouponStatusType()))
                .orderBy(coupon.id.desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();

        JPAQuery<Long> count = queryFactory
                .select(coupon.count())
                .from(coupon)
                .where(getCouponStatusTypeEq(adminCouponListReqDto.getCouponStatusType()));

        return PageableExecutionUtils.getPage(content, pageRequest, count::fetchOne);
    }

    private static BooleanExpression getCouponStatusTypeEq(CouponStatusType couponStatusType) {
        return couponStatusType != null ? coupon.couponStatusType.eq(couponStatusType) : null;
    }
}
