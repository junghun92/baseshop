package com.baseshop.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 비즈니스 에러 코드 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 쿠폰
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 쿠폰이 존재하지 않습니다."),
    COUPON_UNAVAILABLE(HttpStatus.BAD_REQUEST, "발급할 수 없는 쿠폰입니다."),
    COUPON_NOT_STARTED(HttpStatus.CONFLICT, "아직 발급 시작 전입니다."),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 받은 쿠폰입니다.");

    private final HttpStatus httpStatus;
    private final String message;

}
