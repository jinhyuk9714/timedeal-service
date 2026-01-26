package com.timedeal.api.dto.admin;

import com.timedeal.api.domain.user.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 역할 변경 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangeRoleRequest {
    
    @NotNull(message = "역할은 필수입니다.")
    private UserRole role;
}
