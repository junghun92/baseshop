package com.baseshop.board.service;

import com.baseshop.board.dto.*;
import com.baseshop.board.entity.Board;
import com.baseshop.board.repository.BoardQueryRepository;
import com.baseshop.board.repository.BoardRepository;
import com.baseshop.common.dto.PagingResDto;
import com.baseshop.member.repository.MemberRepository;
import com.baseshop.member.entity.Member;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardQueryRepository boardQueryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long memberId, BoardCreateReqDto boardCreateDto) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        Board board = Board.builder()
                .member(member)
                .boardType(boardCreateDto.getBoardType())
                .title(boardCreateDto.getTitle())
                .content(boardCreateDto.getContent())
                .build();

        return boardRepository.save(board).getId();
    }

    @Transactional
    public Long update(Long id, BoardUpdateReqDto boardUpdateDto, Long memberId) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        if(!board.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        board.changeBoard(
                boardUpdateDto.getBoardType(),
                boardUpdateDto.getTitle(),
                boardUpdateDto.getContent());

        return board.getId();
    }

    @Transactional
    public Long delete(Long id, Long memberId) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        if(!board.getMember().getId().equals(memberId)){
            throw new AccessDeniedException("본인이 작성한 글만 삭제할 수 있습니다.");
        }

        boardRepository.delete(board);
        return id;
    }

    @Transactional(readOnly = true)
    public BoardDetailResDto getBoardDetail(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        return BoardDetailResDto.builder()
                .id(board.getId())
                .memberId(board.getMember().getId())
                .boardType(board.getBoardType())
                .title(board.getTitle())
                .content(board.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public PagingResDto<BoardListResDto> getBoardList(BoardListReqDto boardListReqDto) {
        Page<Board> boardList = boardQueryRepository.findBoardList(boardListReqDto);

        return new PagingResDto<>(boardList.map(BoardListResDto::new));
    }
}
