package com.baseshop.common.file.local;

import com.baseshop.common.file.FileStore;
import com.baseshop.common.file.FileType;
import com.baseshop.common.file.FileValidator;
import com.baseshop.common.file.UploadFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("local & !local-r2")   // local이면서 local-r2가 아닐 때만
public class LocalFileStore implements FileStore {

    @Value("${file.dir}")
    private String fileDir;

    @Value("${file.url}")
    private String fileUrl;

    @Override
    public UploadFile createStoreFile(MultipartFile multipartFile, String dir, FileType fileType) {
        FileValidator.validate(multipartFile, fileType);
        UploadFile uploadFile = UploadFile.create(multipartFile, dir);

        try {
            File file = new File(fileDir + uploadFile.getStoreFilePath());
            Files.createDirectories(file.getParentFile().toPath()); // 폴더 없으면 생성
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }

        return uploadFile;
    }

    @Override
    public String getUrl(String storeFilePath) {
        return fileUrl + storeFilePath;
    }

    @Override
    public void deleteStoreFile(UploadFile uploadFile){
        try {
            // 파일이 있으면 지우고, 없으면 넘어감.
            Files.deleteIfExists(Paths.get(fileDir + uploadFile.getStoreFilePath()));
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", uploadFile.getStoreFilePath(), e);   // 삭제 실패로 요청 전체를 실패시키지 않음
        }
    }

}
