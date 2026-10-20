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

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductFileService {

    private static final int RETENTION_DAYS = 7; // 파일 보관기간

    private final FileStore fileStore;
    private final ProductRepository productRepository;
    private final ProductFileRepository productFileRepository;

    @Transactional
    public ProductFileResDto create(Long id, MultipartFile multipartFile) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품입니다."));

        // 기존 이미지는 DB 삭제 표시
        productFileRepository.findByProductIdAndDelYn(id, "N")
                .ifPresent(ProductFile::delete);

        // 파일 업로드
        UploadFile uploadFile = fileStore.createStoreFile(multipartFile, "product", FileType.IMAGE);

        try {
            productFileRepository.saveAndFlush(new ProductFile(product, uploadFile));
        } catch (Exception e) {
            fileStore.deleteStoreFile(uploadFile); // DB오류 발생시 저장한 파일 삭제
            throw new RuntimeException(e);
        }

        return new ProductFileResDto(uploadFile.getOriginalFileName(), fileStore.getUrl(uploadFile.getStoreFilePath()));
    }

    // 상품 상세용: 사용 중인 이미지 URL (없으면 null)
    public String getProductImageUrl(Long productId) {
        return productFileRepository.findByProductIdAndDelYn(productId, "N")
                .map(productFile -> fileStore.getUrl(productFile.getUploadFile().getStoreFilePath()))
                .orElse(null);
    }

    /**
     * 삭제 표시(delYn=Y) 후 7일 지난 상품 이미지 정리
     * - 실제 파일 삭제 + DB 행 삭제
     */
    @Transactional
    public void deleteExpiredFiles() {
        LocalDateTime baseDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<ProductFile> targets = productFileRepository.findAllByDelYnAndDelDateBefore("Y", baseDate);

        for (ProductFile productFile : targets) {
            fileStore.deleteStoreFile(productFile.getUploadFile());   // 실제 파일 삭제
            productFileRepository.delete(productFile);                // DB 행 삭제
        }
        log.info("상품 이미지 정리 완료: {}건", targets.size());
    }


}
