package com.timedeal.api.dto.auth;

import lombok.Getter;

/**
 * 로그인 응답 DTO
 */
@Getter
public class LoginResponse {
    private String token; // JWT 토큰
    private String tokenType; // 토큰 타입

    public LoginResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }
}
