package com.baseshop.board.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum BoardType {

    QNA("QNA", "질문"),
    INFO("INFO", "정보"),
    FREE("FREE", "자유");

    private final String value;
    private final String description;

    @JsonValue
    public String getValue(){
        return value;
    }

    private static final Map<String, BoardType> BoardTypeStringToEnum =
            Stream.of(values()).collect(Collectors.toMap(BoardType::getValue, obj -> obj));

    @JsonCreator
    public static BoardType fromBoardTypeString(final String commandDefineString) {
        return Optional.ofNullable(BoardTypeStringToEnum.get(commandDefineString))
                .orElse(null);
    }
}
