package com.baseshop.product.dto;

import com.baseshop.common.dto.PagingReqDto;
import com.baseshop.product.type.ProductStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductListReqDto extends PagingReqDto {

    @Schema(description = "상품상태(판매중: SALE, 품절: SOLD_OUT, 판매중지: STOP)", example = "SALE")
    private ProductStatusType productStatusType;

}
