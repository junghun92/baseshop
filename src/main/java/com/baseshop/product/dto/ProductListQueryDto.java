package com.baseshop.product.dto;

import com.baseshop.product.entity.Product;
import lombok.Getter;

@Getter
public class ProductListQueryDto {

    private final Product product;
    private final String storeFilePath; // 이미지 없으면 null

    public ProductListQueryDto(Product product, String storeFilePath) {
        this.product = product;
        this.storeFilePath = storeFilePath;
    }
}
