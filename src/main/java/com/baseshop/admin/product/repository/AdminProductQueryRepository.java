package com.baseshop.admin.product.repository;

import com.baseshop.admin.product.dto.AdminProductExcelRowDto;
import com.baseshop.admin.product.dto.AdminProductListReqDto;
import com.baseshop.product.dto.ProductListQueryDto;
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

import static com.baseshop.product.entity.QProduct.product;
import static com.baseshop.product.entity.QProductFile.productFile;

@Repository
@RequiredArgsConstructor
public class AdminProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<ProductListQueryDto> findProductList(AdminProductListReqDto adminProductListReqDto){
        PageRequest pageRequest = adminProductListReqDto.toPageRequest();

        List<Long> idList = queryFactory
                .select(product.id)
                .from(product)
                .where(getProductStatusTypeEq(adminProductListReqDto.getProductStatusType()))
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
                .where(getProductStatusTypeEq(adminProductListReqDto.getProductStatusType()),
                        product.id.in(idList))
                .orderBy(product.id.desc())
                .fetch();

        JPAQuery<Long> count = queryFactory
                .select(product.count())
                .from(product)
                .where(getProductStatusTypeEq(adminProductListReqDto.getProductStatusType()));

        return PageableExecutionUtils.getPage(content, pageRequest, count::fetchOne);
    }

    private static BooleanExpression getProductStatusTypeEq(ProductStatusType productStatusType) {
        return productStatusType != null ? product.productStatusType.eq(productStatusType) : null;
    }

    public List<AdminProductExcelRowDto> findExcelChunk(Long lastProductId, int size) {
        return queryFactory
                .select(Projections.constructor(AdminProductExcelRowDto.class,
                        product.id,
                        product.name,
                        product.price,
                        product.quantity,
                        product.productStatusType,
                        product.description,
                        product.regDate))
                .from(product)
                .where(productIdGt(lastProductId))
                .orderBy(product.id.asc())
                .limit(size)
                .fetch();
    }

    private BooleanExpression productIdGt(Long lastProductId) {
        return lastProductId == null ? null : product.id.gt(lastProductId);
    }
}
