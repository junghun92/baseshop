package com.baseshop.security.token.service;

import com.baseshop.security.token.repository.RefreshTokenRepository;
import com.baseshop.security.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    /** 로그인 성공시 redis 저장 */
    public void createRefreshToken(String username, String refreshToken){
        refreshTokenRepository.save(username, refreshToken);
    }

    /** redis refreshToken 값이랑 검증 */
    public boolean isSameToken(String username, String refreshToken) {
        return refreshTokenRepository.findByUsername(username)
                .map(str -> str.equals(refreshToken))
                .orElse(false);
    }

    /** refreshToken 삭제 */
    public void deleteByRefreshToken(String refreshToken){
        if (refreshToken == null || refreshToken.isBlank()) {
            return;   // 이미 로그아웃된 상태 → 그냥 성공 처리
        }

        //redis 삭제
        String username = jwtUtil.getClaims(refreshToken).getSubject();
        refreshTokenRepository.deleteByUsername(username);
    }


}
