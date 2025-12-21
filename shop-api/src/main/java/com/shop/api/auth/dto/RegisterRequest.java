package com.shop.api.auth.dto;

import com.shop.core.member.dto.CreateMemberCommand;
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

    public CreateMemberCommand toCommand() {
        return CreateMemberCommand.builder()
                .email(this.email)
                .password(this.password)
                .name(this.name)
                .address(this.address)
                .build();
    }
}
