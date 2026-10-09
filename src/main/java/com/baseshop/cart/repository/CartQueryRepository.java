package com.baseshop.cart.repository;

import com.baseshop.cart.dto.CartListReqDto;
import com.baseshop.cart.entity.Cart;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.baseshop.cart.entity.QCart.cart;
import static com.baseshop.product.entity.QProduct.product;

@Repository
@RequiredArgsConstructor
public class CartQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Cart> findCartList(Long memberId, CartListReqDto cartListReqDto){
        PageRequest pageRequest = cartListReqDto.toPageRequest();

        List<Cart> content =  queryFactory
                .select(cart)
                .from(cart)
                .join(cart.product, product).fetchJoin()
                .where(getMemberEq(memberId))
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .orderBy(cart.id.desc())
                .fetch();

        JPAQuery<Long> count = queryFactory
                .select(cart.count())
                .from(cart)
                .where(getMemberEq(memberId));

        return PageableExecutionUtils.getPage(content, pageRequest, count::fetchOne);
    }

    private static BooleanExpression getMemberEq(Long memberId) {
        return cart.member.id.eq(memberId);
    }
}
