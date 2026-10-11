package com.baseshop.member.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.member.dto.MemberCreateReqDto;
import com.baseshop.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
@Tag(name = "회원 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @Operation(summary = "회원가입")
    public ApiResponse<Long> create(@Valid @RequestBody MemberCreateReqDto memberCreateReqDto){
        return ApiResponse.success(memberService.create(memberCreateReqDto));
    }

}
