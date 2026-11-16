package com.baseshop.board.dto;

import com.baseshop.board.type.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BoardRankResDto {

    @Schema(description = "게시글 번호")
    private Long id;

    @Schema(description = "게시판 구분 (QNA: 질문, INFO: 정보, FREE: 자유)", example = "QNA")
    private BoardType boardType;

    @Schema(description = "게시글 제목", example = "테스트 제목입니다.")
    private String title;

    @Schema(description = "게시글 조회수", example = "10")
    private Long viewCount;

    public BoardRankResDto(Long id, BoardType boardType, String title, Long viewCount) {
        this.id = id;
        this.boardType = boardType;
        this.title = title;
        this.viewCount = viewCount;
    }
}
