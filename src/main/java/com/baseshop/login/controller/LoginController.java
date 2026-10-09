package com.baseshop.login.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.login.service.LoginService;
import com.baseshop.member.dto.LoginReqDto;
import com.baseshop.security.util.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "로그인 API")
public class LoginController {

    private final LoginService loginService;
    private final JWTUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "[Swagger Test 용도] LoginFilter가 처리합니다.")
    public String login(@RequestBody LoginReqDto loginReqDto){
        return "LoginFilter에서 처리";
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "${jwt.refresh-token-name}", required = false) String refreshToken,
            HttpServletResponse response) {

        //refreshToken DB 삭제
        loginService.logout(refreshToken);
        
        //refreshToken 쿠키 삭제
        response.addHeader(HttpHeaders.SET_COOKIE, jwtUtil.deleteRefreshCookie().toString());
        
        return ApiResponse.success(null);
    }
}
