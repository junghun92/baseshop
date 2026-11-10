package com.baseshop.admin.product.service;

import com.baseshop.admin.product.repository.AdminProductQueryRepository;
import com.baseshop.common.excel.ExcelFile;
import com.baseshop.admin.product.dto.AdminProductExcelRowDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductExcelService {

    private static final int CHUNK_SIZE = 1000;
    private final AdminProductQueryRepository adminProductQueryRepository;

    public ExcelFile<AdminProductExcelRowDto> createProductExcel(){
        ExcelFile<AdminProductExcelRowDto> excelFile = new ExcelFile<>(AdminProductExcelRowDto.class, "상품");

        try {
            writeAllProduct(excelFile);
            return excelFile;
        } catch (RuntimeException e) {
            excelFile.close();
            throw e;
        }
    }

    private void writeAllProduct(ExcelFile<AdminProductExcelRowDto> excelFile) {
        Long lastProductId = null;

        while (true){
            // 마지막 id 다음부터 1000건
            List<AdminProductExcelRowDto> chunk = adminProductQueryRepository.findExcelChunk(lastProductId, CHUNK_SIZE);
            if (chunk.isEmpty()) {
                break;
            }

            // 엑셀에 쓰기 (메모리엔 100행, 나머지는 임시파일)
            excelFile.writeRows(chunk);

            lastProductId = chunk.get(chunk.size() - 1).getProductId();

            // ④ 1,000건보다 적게 왔으면 마지막 청크
            if (chunk.size() < CHUNK_SIZE) {
                break;
            }
        }
    }
}
