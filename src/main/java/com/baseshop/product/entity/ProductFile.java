package com.baseshop.product.entity;

import com.baseshop.common.entity.BaseEntity;
import com.baseshop.common.file.UploadFile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Embedded
    private UploadFile uploadFile;

    @Column(nullable = false, length = 1)
    private String delYn = "N";

    private LocalDateTime delDate;

    public ProductFile(Product product, UploadFile uploadFile) {
        this.product = product;
        this.uploadFile = uploadFile;
    }

    public void delete(){
        delYn = "Y";
        delDate = LocalDateTime.now();
    }
}
