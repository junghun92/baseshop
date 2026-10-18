package com.baseshop.portone.dto;

import com.baseshop.portone.type.CancelRequesterType;
import lombok.Data;

@Data
public class PortOneCancelReqDto {

    private String reason; // 취소사유 (필수 값)
    private CancelRequesterType requester; // 취소 요청 주체

    public PortOneCancelReqDto(String reason, CancelRequesterType requester) {
        this.reason = reason;
        this.requester = requester;
    }
}
