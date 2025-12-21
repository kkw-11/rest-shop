package com.shop.core.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateMemberCommand {
    private String email;
    private String password;
    private String name;
    private String address;
}