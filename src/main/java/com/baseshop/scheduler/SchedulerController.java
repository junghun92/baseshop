package com.baseshop.scheduler;

import com.baseshop.payment.service.PaymentFacade;
import com.baseshop.product.service.ProductFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerController {

    private final ProductFileService productFileService;
    private final PaymentFacade paymentFacade;

    /**
     * 상품 이미지 삭제 파일 정리 (매일 새벽 4시)
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteProductFiles() {
        productFileService.deleteExpiredFiles();
    }

    /**
     * 결제 대기(READY) 만료 정리 (이전 실행 끝나고 10분 뒤)
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void updateExpiredPayments() {
        paymentFacade.updateExpiredPayments();
    }

}
