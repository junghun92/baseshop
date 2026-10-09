package com.baseshop.common.handler;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Getter
@ToString
@NoArgsConstructor
public class ApiResponse<T> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private boolean status;
    private String statusCode;
    private String message;
    private String responseTime;
    private T data;

    private ApiResponse(boolean status, String statusCode, String message, T data) {
        this.status = status;
        this.statusCode = statusCode;
        this.message = message;
        this.responseTime = LocalDateTime.now().format(formatter);
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "요청이 성공적으로 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(false, errorCode, errorMessage, null);
    }
}
