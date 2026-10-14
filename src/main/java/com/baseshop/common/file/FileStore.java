package com.baseshop.common.file;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

public interface FileStore {

    UploadFile createStoreFile(MultipartFile multipartFile, String dir, FileType fileType);

    void deleteStoreFile(UploadFile uploadFile);

    String getUrl(String storeFilePath);

    // 트랜잭션이 커밋되지 못하고 끝나면 방금 저장한 파일 삭제
    default void deleteFileIfRollback(UploadFile uploadFile) {
        // 트랜잭션 여부 확인
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("파일 저장은 트랜잭션 안에서 호출해야 합니다.");
        }
        
        // 트랜잭션이 끝나고 실행 예약
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // 커밋 성공이 아니면 파일 삭제
                if (status != STATUS_COMMITTED) {
                    deleteStoreFile(uploadFile);   // 구현체(Local/R2)의 삭제가 호출됨
                }
            }
        });
    }

}
