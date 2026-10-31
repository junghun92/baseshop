package com.baseshop.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JWTUtil {

    private final SecretKey key;
    private final long accessTokenExpireTime;
    private final long refreshTokenExpireTime;
    private final String accessTokenName;
    private final String refreshTokenName;
    private final boolean cookieSecure;

    public JWTUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expire-time}") long accessTokenExpireTime,
            @Value("${jwt.refresh-token-expire-time}") long refreshTokenExpireTime,
            @Value("${jwt.access-token-name}") String accessTokenName,
            @Value("${jwt.refresh-token-name}") String refreshTokenName,
            @Value("${jwt.cookie-secure}") boolean cookieSecure) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireTime = accessTokenExpireTime;
        this.refreshTokenExpireTime = refreshTokenExpireTime;
        this.accessTokenName = accessTokenName;
        this.refreshTokenName = refreshTokenName;
        this.cookieSecure = cookieSecure;
    }

    /**
     * accessToken 생성
     */
    public String createAccessToken(Long memberId, String username, String role) {
        return createToken(accessTokenExpireTime, username, memberId, role, accessTokenName);
    }

    /**
     * refreshToken 생성
     */
    public String createRefreshToken(Long memberId, String username, String role) {
        return createToken(refreshTokenExpireTime, username, memberId, role, refreshTokenName);
    }

    private String createToken(long tokenExpireTime, String username, Long memberId, String role, String tokenName) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + tokenExpireTime);

        return Jwts.builder()
                .subject(username)
                .claim("memberId", memberId)
                .claim("role", role)
                .claim("tokenType", tokenName)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    /**
     * JWT 검증 및 읽기
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * refresh Cookie 생성
     */
    public ResponseCookie createRefreshCookie(String refreshToken) {
        return refreshCookie(refreshToken, Duration.ofMillis(refreshTokenExpireTime));
    }

    /**
     * refresh Cookie 삭제 (재발급 실패, 로그아웃)
     */
    public ResponseCookie deleteRefreshCookie() {
        return refreshCookie("", Duration.ZERO);
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(refreshTokenName, value)
                .httpOnly(true)         // 스크립트 접근 차단
                .secure(cookieSecure)          // true 설정시 HTTPS에서만 전송 가능 (로컬 테스트는 false)
                .sameSite("Lax")        // 다른 사이트에서 보낸 POST/fetch 에는 쿠키 안 붙임(CSRF 방어). 프론트·API 같은 도메인이라 Lax로 충분
                                        // None 은 프론트·API 도메인이 다를 때만 사용 (Secure 필수)
                .path("/")             // 주소범위
                .maxAge(maxAge) //유효기간
                .build();
    }

    public String getAccessTokenName() {
        return accessTokenName;
    }

    public String getRefreshTokenName() {
        return refreshTokenName;
    }

}
