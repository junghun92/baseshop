package com.baseshop.product.service;

import com.baseshop.common.excel.ExcelFile;
import com.baseshop.product.dto.ProductExcelRowDto;
import com.baseshop.product.repository.ProductQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductExcelService {

    private static final int CHUNK_SIZE = 1000;
    private final ProductQueryRepository productQueryRepository;

    public ExcelFile<ProductExcelRowDto> createProductExcel(){
        ExcelFile<ProductExcelRowDto> excelFile = new ExcelFile<>(ProductExcelRowDto.class, "상품");

        try {
            writeAllProduct(excelFile);
            return excelFile;
        } catch (RuntimeException e) {
            excelFile.close();
            throw e;
        }
    }

    private void writeAllProduct(ExcelFile<ProductExcelRowDto> excelFile) {
        Long lastProductId = null;

        while (true){
            // 마지막 id 다음부터 1000건
            List<ProductExcelRowDto> chunk = productQueryRepository.findExcelChunk(lastProductId, CHUNK_SIZE);
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
