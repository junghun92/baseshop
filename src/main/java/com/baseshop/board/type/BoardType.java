package com.baseshop.board.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardType {

    QNA("질문"),
    INFO("정보"),
    FREE("자유");

    private final String description;

}
