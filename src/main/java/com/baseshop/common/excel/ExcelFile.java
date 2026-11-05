package com.baseshop.common.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * SXSSF 기반 공통 엑셀 파일
 * - 메모리에는 ROW_ACCESS_WINDOW 행만 두고, 나머지는 임시 파일로 내보냄
 * - HTTP 를 모름 : 출력 스트림(OutputStream)에 쓰기만 함
 * - 다 쓰면 반드시 close() (임시 파일 삭제)
 */
@Slf4j
public class ExcelFile<T> implements AutoCloseable{

    private static final int ROW_ACCESS_WINDOW = 100; // 메모리에 둘 최대 행 수
    private static final int MAX_ROWS = SpreadsheetVersion.EXCEL2007.getMaxRows(); // 엑셀 최대 행
    private static final int MAX_TEXT_LENGTH = SpreadsheetVersion.EXCEL2007.getMaxTextLength();   // 32,767 (셀 최대 글자 수)

    private final SXSSFWorkbook workbook;
    private final Sheet sheet;
    private final List<Field> fields; // @ExcelColumn 필드 목록
    private final CellStyle dateTimeStyle; // 날짜 스타일
    private final CellStyle dateStyle;
    private int rowIndex = 0; // 다음에 쓸 행 번호

    public ExcelFile(Class<T> clazz, String sheetName) {
        this.workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW);
        workbook.setCompressTempFiles(true); // 임시 파일 압축
        this.sheet = workbook.createSheet(sheetName);
        this.fields = extractFields(clazz);
        this.dateTimeStyle = createDateStyle("yyyy-mm-dd hh:mm:ss");
        this.dateStyle = createDateStyle("yyyy-mm-dd");
        writeHeader();
    }
    
    // 행 추가 (청크마다 호출)
    public void writeRows(List<T> rows){
        for (T data : rows) {
            if(rowIndex >= MAX_ROWS){
                throw new IllegalStateException("엑셀 최대 행 수 (" + MAX_ROWS + ") 를 초과 하였습니다.");
            }
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < fields.size(); i++) {
                writeCell(row.createCell(i), readValue(fields.get(i), data));
            }
        }
    }
    
    // 내보내기
    public void writeTo(OutputStream out) throws IOException {
        workbook.write(out);
    }

    // 임시파일 삭제 + 닫기
    @Override
    public void close() {
        try {
            workbook.dispose();
            workbook.close();
        } catch (IOException e) {
            log.warn("엑셀 파일 정리 실패", e);
        }
    }

    // @ExcelColumn 붙은 필드만 골라서 order 순으로 정렬
    private List<Field> extractFields(Class<T> clazz){
        List<Field> result = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ExcelColumn.class))
                .sorted(Comparator.comparingInt(field -> field.getAnnotation(ExcelColumn.class).order()))
                .toList();

        if(result.isEmpty()){
            throw new IllegalArgumentException(clazz.getSimpleName() + "에 @ExcelColumn 필드가 없습니다.");
        }
        result.forEach(field -> field.setAccessible(true)); // private 필드 읽기 허용
        return result;
    }
    
    // 헤더 생성
    private void writeHeader(){
        CellStyle headerStyle = workbook.createCellStyle();
        Font bold = workbook.createFont();
        bold.setBold(true);
        headerStyle.setFont(bold);

        Row header = sheet.createRow(rowIndex++);
        for (int i = 0; i < fields.size(); i++) {
            ExcelColumn column = fields.get(i).getAnnotation(ExcelColumn.class);
            Cell cell = header.createCell(i);
            cell.setCellValue(column.header());
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, column.width() * 256);
        }
    }
    
    // DTO 필드 값 추출
    private Object readValue(Field field, T data){
        try {
            return field.get(data);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("엑셀 값 읽기 실패 : " + field.getName(), e);
        }
    }

    // 값 타입에 맞게 셀에 쓰기
    private void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());   // 숫자 셀 (SUM 가능)
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime);
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof Enum<?> e) {
            cell.setCellValue(e.name());
        } else {
            cell.setCellValue(truncate(value.toString()));
        }
    }

    // 날짜 표시 형식 스타일
    private CellStyle createDateStyle(String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(format));
        return style;
    }

    // 엑셀 셀 최대 글자 수 초과 방지
    private String truncate(String text) {
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
    }
}
