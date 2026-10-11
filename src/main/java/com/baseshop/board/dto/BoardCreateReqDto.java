package com.baseshop.board.dto;

import com.baseshop.board.type.BoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardCreateReqDto {

    @Schema(description = "게시판 구분 (QNA: 질문, INFO: 정보, FREE: 자유)", example = "QNA")
    @NotNull(message = "게시판 구분을 선택해주세요.")
    private BoardType boardType;

    @Schema(description = "게시글 제목", example = "테스트 제목입니다.")
    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    @Schema(description = "게시글 내용", example = "테스트 내용입니다.")
    private String content;

}
