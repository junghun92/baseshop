package com.baseshop.product.repository;

import com.baseshop.board.type.BoardType;
import com.baseshop.product.dto.ProductListReqDto;
import com.baseshop.product.entity.Product;
import com.baseshop.product.entity.QProduct;
import com.baseshop.product.type.ProductStatusType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.baseshop.board.entity.QBoard.board;
import static com.baseshop.product.entity.QProduct.product;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Product> findProductList(ProductListReqDto productListReqDto){
        PageRequest pageRequest = productListReqDto.toPageRequest();

        List<Product> content = queryFactory
                .select(product)
                .from(product)
                .where(getProductStatusTypeEq(productListReqDto.getProductStatusType()))
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .orderBy(product.id.desc())
                .fetch();

        JPAQuery<Long> count = queryFactory
                .select(product.count())
                .from(product)
                .where(getProductStatusTypeEq(productListReqDto.getProductStatusType()));

        return PageableExecutionUtils.getPage(content, pageRequest, count::fetchOne);
    }

    private static BooleanExpression getProductStatusTypeEq(ProductStatusType productStatusType) {
        return productStatusType != null ? product.productStatusType.eq(productStatusType) : null;
    }
}
