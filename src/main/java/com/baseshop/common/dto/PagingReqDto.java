package com.baseshop.common.dto;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class PagingReqDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private int page;
    private int size;

    public PageRequest toPageRequest(){
        return toPageRequest(Sort.by("id").descending());
    }

    public PageRequest toPageRequest(Sort sort){
        int page = Math.max(this.page, DEFAULT_PAGE);
        int size = (this.size <= 0) ? DEFAULT_SIZE : Math.min(this.size, MAX_SIZE);

        return PageRequest.of(page-1, size, sort);
    }
}
