package com.shop.rest.commerce.dto;

import com.shop.rest.commerce.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private String code;
    private T data;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공했습니다.", null, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, null, data);
    }

    // 실패 응답 (ErrorCode 사용)
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getMessage(), errorCode.getCode(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(false, errorCode.getMessage(), errorCode.getCode(), data);
    }

    // 실패 응답 (커스텀 메시지)
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, message, code, null);
    }
}