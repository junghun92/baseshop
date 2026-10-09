package com.baseshop.board.dto;

import com.baseshop.board.type.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
public class BoardDetailResDto {

    @Schema(description = "게시글 번호")
    private Long id;

    @Schema(description = "회원 번호")
    private Long memberId;

    @Schema(description = "게시판 구분 (QNA: 질문, INFO: 정보, FREE: 자유)", example = "QNA")
    private BoardType boardType;

    @Schema(description = "게시글 제목", example = "테스트 제목입니다.")
    private String title;

    @Schema(description = "게시글 내용", example = "테스트 내용입니다.")
    private String content;

    @Builder
    public BoardDetailResDto(Long id, Long memberId, BoardType boardType, String title, String content) {
        this.id = id;
        this.memberId = memberId;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
    }
}
