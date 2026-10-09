package com.baseshop.security.token.controller;

import com.baseshop.common.handler.ApiResponse;
import com.baseshop.security.token.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "토큰 API")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping("/reissue")
    @Operation(summary = "Access Token 재발급")
    public ApiResponse<Void> reissue(
            @CookieValue(name = "${jwt.refresh-token-name}", required = false) String refreshToken,
            HttpServletResponse response){

        String newAccessToken = tokenService.reissue(refreshToken); // 실패시 JwtException
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);
        return ApiResponse.success(null);
    }
}
