package com.baseshop.member.dto;

import com.baseshop.member.type.MemberRoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
public class MemberDetailResDto {

    @Schema(description = "회원번호")
    private Long id;

    @Schema(description = "아이디")
    private String username;

    @Schema(description = "패스워드")
    private String password;

    @Schema(description = "회원권한")
    private MemberRoleType memberRoleType;

    @Builder
    public MemberDetailResDto(Long id, String username, String password, MemberRoleType memberRoleType) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.memberRoleType = memberRoleType;
    }
}
