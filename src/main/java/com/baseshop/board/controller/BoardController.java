package com.baseshop.board.controller;

import com.baseshop.board.dto.*;
import com.baseshop.board.service.BoardService;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.common.handler.ApiResponse;
import com.baseshop.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/board")
@Tag(name = "게시판 API")
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    @Operation(summary = "게시글 등록")
    public ApiResponse<Long> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BoardCreateReqDto boardCreateDto){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(boardService.create(memberId, boardCreateDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "게시글 수정")
    public ApiResponse<Long> update(
            @PathVariable Long id,
            @Valid @RequestBody BoardUpdateReqDto boardUpdateDto,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(boardService.update(id, boardUpdateDto, memberId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "게시글 삭제")
    public ApiResponse<Long> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        Long memberId = userDetails.getMemberId();

        return ApiResponse.success(boardService.delete(id, memberId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "게시글 상세")
    public ApiResponse<BoardDetailResDto> getBoardDetail(
            @PathVariable Long id){

        return ApiResponse.success(boardService.getBoardDetail(id));
    }

    @GetMapping
    @Operation(summary = "게시글 리스트")
    public ApiResponse<PagingResDto<BoardListResDto>> getBoardList(
            BoardListReqDto boardListReqDto){
        return ApiResponse.success(boardService.getBoardList(boardListReqDto));
    }
}
