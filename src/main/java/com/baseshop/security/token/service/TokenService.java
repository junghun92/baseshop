package com.baseshop.security.token.service;

import com.baseshop.security.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
        Long memberId = claims.get("memberId", Long.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String tokenType = claims.get("tokenType", String.class);

        // refreshToken 토큰인지 확인
        if(!jwtUtil.getRefreshTokenName().equals(tokenType)){
            throw new JwtException("refreshToken이 아닙니다.");
        }

        // redis refreshToken 저장 값 이랑 비교
        if(!refreshTokenService.isSameToken(username, refreshToken)){
            refreshTokenService.deleteByRefreshToken(refreshToken); // 강제 로그아웃
            throw new JwtException("저장 된 refreshToken과 일치하지 않습니다.");
        }

        // accessToken 재발급
        return jwtUtil.createAccessToken(memberId, username, role);
    }
}
