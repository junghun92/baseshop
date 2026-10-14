package com.baseshop.common.file;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadFile {

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, length = 100, unique = true)
    private String storeFileName;

    @Column(nullable = false)
    private String storeFilePath;

    @Column(nullable = false, length = 10)
    private String ext;

    @Column(nullable = false)
    private long fileSize;

    @Builder
    public UploadFile(String originalFileName, String storeFileName, String storeFilePath, String ext, long fileSize) {
        this.originalFileName = originalFileName;
        this.storeFileName = storeFileName;
        this.storeFilePath = storeFilePath;
        this.ext = ext;
        this.fileSize = fileSize;
    }

    // 업로드 파일 정보 생성 (product/2026/10/uuid.png)
    public static UploadFile create(MultipartFile multipartFile, String dir) {
        String originalFilename = multipartFile.getOriginalFilename();
        String ext = FileValidator.getExt(originalFilename);
        String storeFileName = getStoreFileName(ext);
        String storeFilePath = getStoreFilePath(dir, storeFileName);

        return UploadFile.builder()
                .originalFileName(originalFilename)
                .storeFileName(storeFileName)
                .storeFilePath(storeFilePath)
                .ext(ext)
                .fileSize(multipartFile.getSize())
                .build();
    }

    private static String getStoreFilePath(String dir, String storeFileName) {
        return dir + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/" + storeFileName;
    }

    private static String getStoreFileName(String ext) {
        return UUID.randomUUID().toString() + "." + ext;
    }

}
