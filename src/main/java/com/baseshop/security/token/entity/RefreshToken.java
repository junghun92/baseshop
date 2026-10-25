package com.baseshop.security.token.entity;

import com.baseshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String refreshToken;

    @Column(nullable = false, unique = true) //회원당 1개
    private String username;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder
    public RefreshToken(String refreshToken, String username, LocalDateTime expiryDate) {
        this.refreshToken = refreshToken;
        this.username = username;
        this.expiryDate = expiryDate;
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime expiryDate) {
        this.refreshToken = refreshToken;
        this.expiryDate = expiryDate;
    }
}
