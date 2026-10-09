package com.baseshop.login.service;

import com.baseshop.security.token.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LoginService {

    private final RefreshTokenService refreshTokenService;

    /**
     * DB에서 refreshToken 삭제
     * 토큰이 없을경우 return (이미 로그아웃)
     */
    public void logout(String refreshToken){
        if(refreshToken == null || refreshToken.isBlank()){
            return;
        }

        refreshTokenService.deleteByRefreshToken(refreshToken);
    }
}
