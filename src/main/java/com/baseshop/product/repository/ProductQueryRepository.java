package com.baseshop.product.repository;

import com.baseshop.product.dto.ProductListQueryDto;
import com.baseshop.product.dto.ProductListReqDto;
import com.baseshop.product.type.ProductStatusType;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.baseshop.product.entity.QProduct.product;
import static com.baseshop.product.entity.QProductFile.productFile;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<ProductListQueryDto> findProductList(ProductListReqDto productListReqDto){
        PageRequest pageRequest = productListReqDto.toPageRequest();

        List<Long> idList = queryFactory
                .select(product.id)
                .from(product)
                .where(product.productStatusType.in(ProductStatusType.USER_VISIBLE))
                .orderBy(product.id.desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();

        // productFile 사용중인 이미지는 최대 1개라서 조인으로 가져오도록 함.
        List<ProductListQueryDto> content = queryFactory
                .select(Projections.constructor(ProductListQueryDto.class,
                        product,
                        productFile.uploadFile.storeFilePath))
                .from(product)
                .leftJoin(productFile)
                .on(productFile.product.eq(product),
                        productFile.delYn.eq("N")) //사용중인 이미지만
                .where(product.productStatusType.in(ProductStatusType.USER_VISIBLE),
                        product.id.in(idList))
                .orderBy(product.id.desc())
                .fetch();

        JPAQuery<Long> count = queryFactory
                .select(product.count())
                .from(product)
                .where(product.productStatusType.in(ProductStatusType.USER_VISIBLE));

        return PageableExecutionUtils.getPage(content, pageRequest, count::fetchOne);
    }

}
