package com.baseshop.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Schema(description = "페이징 데이터")
public class PagingResDto<T> {

    @Schema(description = "조회된 데이터 목록")
    private final List<T> content;

    @Schema(description = "현재 페이지 번호 (1부터 시작)")
    private int currentPage;

    @Schema(description = "페이지당 데이터 개수 (조회 개수)")
    private int pageSize;

    @Schema(description = "전체 데이터 개수 (총 글 수)")
    private long totalElements;

    @Schema(description = "전체 페이지 수 (총 페이지)")
    private int totalPages;

    @Schema(description = "이전 페이지 존재 여부 (true: 존재함, false: 없음)")
    private boolean hasPrevious;

    @Schema(description = "다음 페이지 존재 여부 (true: 존재함, false: 없음)")
    private boolean hasNext;

    @Schema(description = "첫 페이지 여부 (true: 첫페이지 맞음, false: 아님)")
    private boolean firstPage;

    @Schema(description = "마지막 페이지 여부 (true: 마지막페이지 맞음, false: 아님)")
    private boolean lastPage;

    //JPA
    public PagingResDto(Page<T> page) {
        this.content = page.getContent();
        this.currentPage = page.getNumber() + 1;
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.hasPrevious = page.hasPrevious();
        this.hasNext = page.hasNext();
        this.firstPage = page.isFirst();
        this.lastPage = page.isLast();
    }

}
