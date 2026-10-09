package com.baseshop.order.dto;

import com.baseshop.common.dto.PagingReqDto;
import com.baseshop.order.type.OrderStatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderListReqDto extends PagingReqDto {

    @Schema(description = "주문상태 (ORDER: 주문, CANCEL: 취소)", example = "ORDER")
    private OrderStatusType orderStatusType;

}
