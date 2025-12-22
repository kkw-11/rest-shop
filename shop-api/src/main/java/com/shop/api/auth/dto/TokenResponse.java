package com.shop.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 응답
 */
@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;

    public static TokenResponse of(String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();

    }
}
