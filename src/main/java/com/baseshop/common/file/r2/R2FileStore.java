package com.baseshop.common.file.r2;

import com.baseshop.common.file.FileStore;
import com.baseshop.common.file.FileType;
import com.baseshop.common.file.FileValidator;
import com.baseshop.common.file.UploadFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@Profile("prod | local-r2")   // 운영이거나, 로컬 R2 테스트일 때
@RequiredArgsConstructor
public class R2FileStore implements FileStore {

    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.public-url}")
    private String publicUrl;   // https://img.내도메인.com/

    @Override
    public UploadFile createStoreFile(MultipartFile multipartFile, String dir, FileType fileType) {
        FileValidator.validate(multipartFile, fileType);
        UploadFile uploadFile = UploadFile.create(multipartFile, dir);

        // ===== R2에 업로드 (구현체마다 다른 부분) =====
        // Content-Type을 지정해야 브라우저가 다운로드하지 않고 이미지로 표시함
        String contentType = MediaTypeFactory.getMediaType(uploadFile.getStoreFileName())
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uploadFile.getStoreFilePath())   // DB의 storeFilePath가 그대로 R2 키
                .contentType(contentType)
                .contentLength(multipartFile.getSize())
                .build();

        try (InputStream inputStream = multipartFile.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
        } catch (IOException | SdkException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }

        return uploadFile;
    }

    @Override
    public void deleteStoreFile(UploadFile uploadFile) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(uploadFile.getStoreFilePath())
                    .build());
        } catch (SdkException e) {
            log.warn("파일 삭제 실패: {}", uploadFile.getStoreFilePath(), e);
        }
    }

    @Override
    public String getUrl(String storeFilePath) {
        return publicUrl + storeFilePath;
    }


}
