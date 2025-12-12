package com.shop.rest.commerce.constant;

import lombok.Getter;

@Getter
public enum ResponseMessage {
    MEMBER_REGISTERED("회원가입이 완료되었습니다."),
    MEMBER_UPDATED("회원 정보가 수정되었습니다.");

    private final String message;

    ResponseMessage(String message) {
        this.message = message;
    }

}

