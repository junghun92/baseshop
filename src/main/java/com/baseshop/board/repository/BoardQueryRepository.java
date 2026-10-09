package com.baseshop.board.repository;

import com.baseshop.board.dto.BoardListReqDto;
import com.baseshop.board.entity.Board;
import com.baseshop.board.type.BoardType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.baseshop.board.entity.QBoard.board;

@Repository
@RequiredArgsConstructor
public class BoardQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Board> findBoardList(BoardListReqDto boardListReqDto){
        PageRequest pageRequest = boardListReqDto.toPageRequest();

        List<Board> content = queryFactory
                .select(board)
                .from(board)
                .where(getBoardTypeEq(boardListReqDto.getBoardType()))
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .orderBy(board.id.desc())
                .fetch();

        JPAQuery<Long> count = queryFactory
                .select(board.count())
                .from(board)
                .where(getBoardTypeEq(boardListReqDto.getBoardType()));

        return PageableExecutionUtils.getPage(content, pageRequest, count::fetchOne);
    }

    private static BooleanExpression getBoardTypeEq(BoardType boardType) {
        return boardType != null ? board.boardType.eq(boardType) : null;
    }


}
