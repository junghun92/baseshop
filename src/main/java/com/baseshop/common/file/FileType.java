package com.baseshop.common.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum FileType {

    IMAGE(Set.of("jpg", "jpeg", "png", "gif", "webp"), "image/"),
    EXCEL(Set.of("xlsx"), null);   // 엑셀은 브라우저마다 Content-Type이 달라서 검사 안 함

    private final Set<String> allowedExts;      // 허용 확장자
    private final String contentTypePrefix;     // null이면 Content-Type 검사 생략

}
