package com.baseshop.security.token.service;

import com.baseshop.security.token.entity.RefreshToken;
import com.baseshop.security.token.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpireTime;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expire-time}") long refreshTokenExpireTime) {

        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpireTime = refreshTokenExpireTime;
    }

    /**
     * 로그인 성공시
     * refreshToken DB에 저장 or 업데이트 
     * (배치에서 기존정보 삭제 안되었을 경우가 있기때문에)
     */

    @Transactional
    public void createOrUpdateRefreshToken(String username, String refreshToken){
        //만료시간
        LocalDateTime expiryDate = LocalDateTime.now().plus(refreshTokenExpireTime, ChronoUnit.MILLIS);

        RefreshToken result = refreshTokenRepository.findByUsername(username).orElse(null);

        // 값이 없으면 insert
        if(result == null) {
            RefreshToken data = RefreshToken.builder()
                    .username(username)
                    .refreshToken(refreshToken)
                    .expiryDate(expiryDate)
                    .build();

            refreshTokenRepository.save(data);
            return;
        }

        // 값이 있으면 update
        result.updateRefreshToken(refreshToken, expiryDate);
    }

    /**
     * DB에 저장된 refreshToken 값이랑 같은지 확인
     */
    @Transactional(readOnly = true)
    public boolean isSameToken(String username, String refreshToken) {
        return refreshTokenRepository.findByUsername(username)
                .map(saved -> saved.getRefreshToken().equals(refreshToken))
                .orElse(false);
    }

    @Transactional
    public void deleteByRefreshToken(String refreshToken){
        refreshTokenRepository.deleteByRefreshToken(refreshToken);
    }


}
