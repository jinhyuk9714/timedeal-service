package com.timedeal.api.dto.user;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "사용자 정보 응답")
@Getter
public class UserResponse {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    
    @Schema(description = "이메일 주소", example = "user@example.com")
    private String email;
    
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
    
    @Schema(description = "사용자 역할", example = "USER", allowableValues = {"USER", "ADMIN"})
    private UserRole role;
    
    @Schema(description = "생성 일시", example = "2026-01-27T08:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시", example = "2026-01-27T08:00:00")
    private LocalDateTime updatedAt;
    
    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
