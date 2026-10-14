package com.baseshop.scheduler;

import com.baseshop.common.file.FileStore;
import com.baseshop.product.entity.ProductFile;
import com.baseshop.product.repository.ProductFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerController {

    private static final int RETENTION_DAYS = 7; // 파일 보관기간

    private final ProductFileRepository productFileRepository;
    private final FileStore fileStore;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteProductFiles() {
        LocalDateTime baseDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<ProductFile> targets = productFileRepository.findAllByDelYnAndDelDateBefore("Y", baseDate);

        for (ProductFile productFile : targets) {
            fileStore.deleteStoreFile(productFile.getUploadFile());   // 실제 파일 삭제
            productFileRepository.delete(productFile);                // DB 행 삭제
        }
        log.info("상품 이미지 정리 완료: {}건", targets.size());
    }

}
