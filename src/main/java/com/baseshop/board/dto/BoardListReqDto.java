package com.baseshop.board.dto;

import com.baseshop.board.type.BoardType;
import com.baseshop.common.dto.PagingReqDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BoardListReqDto extends PagingReqDto {

    @Schema(description = "게시판 구분 (QNA: 질문, INFO: 정보, FREE: 자유)", example = "QNA")
    private BoardType boardType;

}
