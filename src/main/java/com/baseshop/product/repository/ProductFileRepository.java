package com.baseshop.product.repository;

import com.baseshop.product.entity.ProductFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductFileRepository extends JpaRepository<ProductFile, Long> {

    Optional<ProductFile> findByProductIdAndDelYn(Long productId, String delYn);

    // 배치 대상: 삭제 표시 후 기준일이 지난 것
    List<ProductFile> findAllByDelYnAndDelDateBefore(String delYn, LocalDateTime baseDate);

    List<ProductFile> findByProductIdInAndDelYn(List<Long> productIdList, String delYn);
}
