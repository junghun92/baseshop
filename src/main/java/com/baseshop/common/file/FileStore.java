package com.baseshop.common.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStore {

    UploadFile createStoreFile(MultipartFile multipartFile, String dir, FileType fileType);

    void deleteStoreFile(UploadFile uploadFile);

    String getUrl(String storeFilePath);

}
