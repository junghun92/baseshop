package com.baseshop.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
public class MemberCreateReqDto {

    @Schema(description = "유저 아이디")
    @NotBlank(message = "유저 아이디를 입력해주세요.")
    private String username;

    @Schema(description = "유저 패스워드")
    @NotBlank(message = "유저 패스워드를 입력해주세요.")
    private String password;

    @NotBlank(message = "유저 이메일을 입력해주세요.")
    @Schema(description = "유저 이메일")
    private String email;
}
