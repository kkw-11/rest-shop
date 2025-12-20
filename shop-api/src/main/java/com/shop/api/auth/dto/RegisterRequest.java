package com.shop.api.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 가입 요청
 */
@Getter
@NoArgsConstructor
public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    private String address;
}
