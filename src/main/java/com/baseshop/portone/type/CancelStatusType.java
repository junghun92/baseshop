package com.baseshop.portone.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CancelStatusType {

    FAILED("취소실패"),
    REQUESTED("취소요청"),
    SUCCEEDED("취소완료");

    private final String description;

}
