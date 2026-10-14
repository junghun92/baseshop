package com.baseshop.common.exception;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.security.util.JWTUtil;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final JWTUtil jwtUtil;

    /**
     * [400 BAD_REQUEST] @Valid 유효성 검증 실패 시 잡아내는 에러 핸들러
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값이 올바르지 않습니다.");

        log.warn("Validation 에러 발생: {}", message);
        return ApiResponse.fail("INVALID_INPUT_VALUE", message);
    }

    /**
     * [409 CONFLICT] 락 대기시간 초과 / 데드락 (동시 주문 몰림)
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handlePessimisticLockingFailureException(PessimisticLockingFailureException e) {
        log.warn("락 획득 실패: {}", e.getMessage());
        return ApiResponse.fail("LOCK_CONFLICT", "주문이 몰려 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }


    /**
     * [413 PAYLOAD_TOO_LARGE] 파일 사이즈 오류
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ApiResponse.fail("PAYLOAD_TOO_LARGE", "파일 크기는 최대 5MB까지 업로드할 수 있습니다.");
    }

    /**
     * [400 BAD_REQUEST] 요청 JSON을 읽을 수 없을 때 (형식 오류, 필수 숫자 누락, 잘못된 enum 값 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("요청 형식 오류: {}", e.getMessage());
        return ApiResponse.fail("INVALID_INPUT_VALUE", "요청 형식이 올바르지 않습니다.");
    }

    /**
     * [400 BAD_REQUEST] 비즈니스 로직 상 잘못된 요청이 들어왔을 때 가로챕니다.
     * (예: 재고 부족, 권한 없음 등 서비스 레이어에서 throw new IllegalArgumentException 던질 때 대응)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("비즈니스 로직 에러 발생: {}", e.getMessage());
        return ApiResponse.fail("BAD_REQUEST", e.getMessage());
    }

    /**
     * [404 NOT_FOUND] JPA 조회 시 데이터(게시글, 회원 등)가 존재하지 않을 때 가로챕니다.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // HTTP 상태코드 404 강제 지정
    public ApiResponse<Void> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("조회 실패 에러 발생: {}", e.getMessage());
        return ApiResponse.fail("ENTITY_NOT_FOUND", e.getMessage());
    }

    /**
     * [403 FORBIDDEN] 권한없음
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("권한 없음: {}", e.getMessage());
        return ApiResponse.fail("FORBIDDEN", e.getMessage());
    }

    /**
     * [401 UNAUTHORIZED] 토큰 재발급 실패 (refreshToken 없음·만료·위조·DB 불일치)
     * refreshToken 쿠키를 삭제하고, 프론트는 로그인 화면으로 이동
     */
    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleJwtException(JwtException e, HttpServletResponse response) {
        log.warn("토큰 재발급 실패: {}", e.getMessage());
        response.addHeader(HttpHeaders.SET_COOKIE, jwtUtil.deleteRefreshCookie().toString());
        return ApiResponse.fail("REISSUE_FAILED", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> exception(Exception e) {
        log.error("Exception {}", e.getMessage());
        return ApiResponse.fail("INTERNAL_SERVER_ERROR", "서버 내부 에러가 발생했습니다. 관리자에게 문의하세요.");
    }

}
