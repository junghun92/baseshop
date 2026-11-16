package com.baseshop.board.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardViewCount {

    @Id
    private Long boardId;
    private Long viewCount;

    public static BoardViewCount init(Long boardId, Long viewCount) {
        BoardViewCount boardViewCount = new BoardViewCount();
        boardViewCount.boardId = boardId;
        boardViewCount.viewCount = viewCount;
        return boardViewCount;
    }
}
