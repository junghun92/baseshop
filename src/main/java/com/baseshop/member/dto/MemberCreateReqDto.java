package com.baseshop.member.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class MemberCreateReqDto {

    private String username;
    private String password;
    private String email;
}
