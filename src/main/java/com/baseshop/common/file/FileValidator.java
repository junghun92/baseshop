package com.baseshop.common.file;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class FileValidator {

    public static void validate(MultipartFile multipartFile, FileType fileType) {
        validateNotEmpty(multipartFile); //빈파일 검증
        validateExt(multipartFile, fileType.getAllowedExts()); //확장자 검증
        
        if (fileType.getContentTypePrefix() != null) { // Content-Type 검증 (엑셀은 브라우저마다 Content-Type이 달라서 검사 안 함)
            validateContentType(multipartFile, fileType.getContentTypePrefix());
        }
    }

    // 빈파일 검증
    private static void validateNotEmpty(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("파일이 존재하지 않습니다.");
        }
    }

    // 확장자 검증
    private static void validateExt(MultipartFile multipartFile, Set<String> allowedExts) {
        String ext = getExt(multipartFile.getOriginalFilename());
        if (!allowedExts.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (허용: " + allowedExts + ")");
        }
    }

    // Content-Type 검증
    private static void validateContentType(MultipartFile multipartFile, String typePrefix) {
        String contentType = multipartFile.getContentType();
        if (contentType == null || !contentType.startsWith(typePrefix)) {
            throw new IllegalArgumentException("파일 형식이 올바르지 않습니다.");
        }
    }

    // 확장자 추출
    public static String getExt(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("확장자가 없는 파일입니다.");
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }


}
