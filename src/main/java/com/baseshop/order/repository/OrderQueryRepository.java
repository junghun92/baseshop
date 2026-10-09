package com.baseshop.order.repository;

import com.baseshop.member.entity.Member;
import com.baseshop.order.dto.OrderListReqDto;
import com.baseshop.order.entity.Order;
import com.baseshop.order.entity.QOrder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.baseshop.order.entity.QOrder.order;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Order> findOrderList(Long memberId, OrderListReqDto orderListReqDto){
        PageRequest pageRequest = orderListReqDto.toPageRequest();

        List<Order> content = queryFactory
                .select(order)
                .from(order)
                .where(order.member.id.eq(memberId))
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .orderBy(order.id.desc())
                .fetch();

        JPAQuery<Long> count = queryFactory
                .select(order.count())
                .from(order)
                .where(order.member.id.eq(memberId));

        return PageableExecutionUtils.getPage(content, pageRequest, count::fetchOne);
    }
}
