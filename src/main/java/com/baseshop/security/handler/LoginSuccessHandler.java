package com.baseshop.security.handler;

import com.baseshop.security.util.JWTUtil;
import com.baseshop.security.token.service.RefreshTokenService;
import com.baseshop.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * 토큰 발급을 위해 구현
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberId = userDetails.getMemberId();

        // JWT 생성
        String accessToken = jwtUtil.createAccessToken(memberId, username, role);
        String refreshToken = jwtUtil.createRefreshToken(memberId, username, role);

        // Refresh Token 저장
        refreshTokenService.createRefreshToken(username, refreshToken);

        // Refresh Token Cookie 생성 (재발급 API에만 쿠키 전송되도록)
        ResponseCookie refreshCookie = jwtUtil.createRefreshCookie(refreshToken);

        // Refresh Token Cookie 전송
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        
        // Access Token 전송
        response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
    }

}
