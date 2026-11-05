package com.baseshop.common.excel;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 엑셀 다운로드 응답 생성
 * - 엑셀은 이미 완성된 상태로 받음 (생성 중 에러는 이미 서비스에서 처리됨)
 * - 여기서는 헤더 설정 + 전송 + 전송 후 정리만
 */
public class ExcelUtils {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private ExcelUtils() {   // 유틸 클래스 : 객체 생성 막기
    }

    public static <T> ResponseEntity<StreamingResponseBody> toResponse(String fileName, ExcelFile<T> excelFile){
        StreamingResponseBody body = out -> {
            try {
                excelFile.writeTo(out);
            } finally {
                excelFile.close();
            }
        };

        // 파일명 (ex:상품목록_20261105.xlsx)
        String fullName = fileName + "_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fullName, StandardCharsets.UTF_8)
                .build();
        
        // 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(XLSX)
                .body(body);
    }
}
