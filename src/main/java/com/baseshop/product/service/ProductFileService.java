package com.baseshop.product.service;

import com.baseshop.common.file.FileStore;
import com.baseshop.common.file.FileType;
import com.baseshop.common.file.UploadFile;
import com.baseshop.product.dto.ProductFileResDto;
import com.baseshop.product.entity.Product;
import com.baseshop.product.entity.ProductFile;
import com.baseshop.product.repository.ProductFileRepository;
import com.baseshop.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductFileService {

    private final FileStore fileStore;
    private final ProductRepository productRepository;
    private final ProductFileRepository productFileRepository;

    @Transactional
    public ProductFileResDto createProductFile(Long id, MultipartFile multipartFile) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

        // 기존 이미지는 DB 삭제 표시
        productFileRepository.findByProductIdAndDelYn(id, "N")
                .ifPresent(ProductFile::delete);

        // 파일 업로드
        UploadFile uploadFile = fileStore.createStoreFile(multipartFile, "product", FileType.IMAGE);

        ProductFile productFile = new ProductFile(product, uploadFile);
        productFileRepository.save(productFile);

        return new ProductFileResDto(uploadFile.getOriginalFileName(), fileStore.getUrl(uploadFile.getStoreFilePath()));
    }

    // 상품 상세용: 사용 중인 이미지 URL (없으면 null)
    public String getProductImageUrl(Long productId) {
        return productFileRepository.findByProductIdAndDelYn(productId, "N")
                .map(productFile -> fileStore.getUrl(productFile.getUploadFile().getStoreFilePath()))
                .orElse(null);
    }

}
