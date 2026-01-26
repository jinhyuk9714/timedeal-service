package com.timedeal.api.dto.user;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {
    
    private Long id;
    private String email;
    private String name;
    private UserRole role; // 사용자 역할
    private LocalDateTime createdAt;
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
