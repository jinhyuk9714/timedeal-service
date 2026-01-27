package com.timedeal.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 */
@Schema(description = "로그인 응답")
@Getter
public class LoginResponse {
    
    @Schema(description = "JWT 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "사용자 역할", example = "USER", allowableValues = {"USER", "ADMIN"})
    private String role;

    public LoginResponse(String token, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.role = role;
    }
}
