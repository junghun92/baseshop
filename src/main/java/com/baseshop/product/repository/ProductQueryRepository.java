package com.baseshop.product.repository;

import com.baseshop.board.type.BoardType;
import com.baseshop.product.dto.ProductListQueryDto;
import com.baseshop.product.dto.ProductListReqDto;
import com.baseshop.product.entity.Product;
import com.baseshop.product.entity.QProduct;
import com.baseshop.product.entity.QProductFile;
import com.baseshop.product.type.ProductStatusType;
import com.querydsl.core.types.Projections;
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
import static com.baseshop.product.entity.QProductFile.productFile;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<ProductListQueryDto> findProductList(ProductListReqDto productListReqDto){
        PageRequest pageRequest = productListReqDto.toPageRequest();

        // productFile 사용중인 이미지는 최대 1개라서 조인으로 가져오도록 함.
        List<ProductListQueryDto> content = queryFactory
                .select(Projections.constructor(ProductListQueryDto.class,
                        product,
                        productFile.uploadFile.storeFilePath))
                .from(product)
                .leftJoin(productFile)
                    .on(productFile.product.eq(product),
                        productFile.delYn.eq("N")) //사용중인 이미지만
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
