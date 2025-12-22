package com.shop.api.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AccessToken 갱신 요청
 */
@Getter
@NoArgsConstructor
public class TokenRefreshRequest {
    private String refreshToken;
}
