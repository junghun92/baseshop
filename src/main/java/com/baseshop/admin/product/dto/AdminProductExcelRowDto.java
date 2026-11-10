package com.baseshop.admin.product.dto;

import com.baseshop.common.excel.ExcelColumn;
import com.baseshop.product.type.ProductStatusType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AdminProductExcelRowDto {
    
    @ExcelColumn(header = "상품번호", order = 1, width = 10)
    private Long productId;

    @ExcelColumn(header = "상품명", order = 2, width = 30)
    private String name;
    
    @ExcelColumn(header = "가격", order = 3, width = 12)
    private int price;

    @ExcelColumn(header = "재고", order = 4, width = 10)
    private int quantity;

    @ExcelColumn(header = "판매상태", order = 5, width = 10)
    private String productStatusName;

    @ExcelColumn(header = "상품설명", order = 6, width = 60)
    private String description;

    @ExcelColumn(header = "등록일", order = 7, width = 20)
    private LocalDateTime regDate;

    public AdminProductExcelRowDto(Long productId, String name, int price, int quantity,
                                   ProductStatusType productStatusType, String description, LocalDateTime regDate) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.productStatusName = productStatusType.getDescription();
        this.description = description;
        this.regDate = regDate;
    }
}
