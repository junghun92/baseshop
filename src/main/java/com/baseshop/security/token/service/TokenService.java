package com.baseshop.security.token.service;

import com.baseshop.security.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public TokenService(
            JWTUtil jwtUtil,
            RefreshTokenService refreshTokenService) {

        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * refreshToken 검증 후 accessToken, refreshToken 새로 발급
     */
    @Transactional
    public String reissue(String refreshToken){
        // cookie에 refreshToken 있는지 체크
        if(refreshToken == null || refreshToken.isBlank()){
            throw new JwtException("refreshToken이 없습니다.");
        }
        
        // 서명,만료 검증
        Claims claims = jwtUtil.getClaims(refreshToken);

        // refreshToken 토큰인지 확인
        if(!jwtUtil.getRefreshTokenName().equals(claims.get("tokenType", String.class))){
            throw new JwtException("refreshToken이 아닙니다.");
        }

        Long memberId = claims.get("memberId", Long.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);


        // DB에 저장된 refreshToken 값이랑 같은지 확인 (로그아웃했거나 이미 교체된 토큰 차단)
        boolean sameToken = refreshTokenService.isSameToken(username, refreshToken);
        if(!sameToken){
            throw new JwtException("저장된 refreshToken과 일치하지 않습니다.");
        }

        // accessToken 재발급
        String newAccessToken = jwtUtil.createAccessToken(memberId, username, role);

        return newAccessToken;
    }
}
