package com.baseshop.member.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum MemberRoleType {

    USER("USER"),
    ADMIN("ADMIN");

    private final String value;

    @JsonValue
    public String getValue(){
        return value;
    }

    private static final Map<String, MemberRoleType> MemberRoleTypeStringToEnum =
            Stream.of(values()).collect(Collectors.toMap(MemberRoleType::getValue, obj -> obj));

    @JsonCreator
    public static MemberRoleType fromMemberRoleTypeString(final String commandDefineString) {
        return Optional.ofNullable(MemberRoleTypeStringToEnum.get(commandDefineString))
                .orElse(null);
    }
}
