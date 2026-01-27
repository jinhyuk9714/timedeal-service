package com.timedeal.api.dto.admin;

import com.timedeal.api.domain.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 역할 변경 요청 DTO
 */
@Schema(description = "사용자 역할 변경 요청")
@Getter
@Setter
@NoArgsConstructor
public class ChangeRoleRequest {
    
    @Schema(description = "변경할 역할", example = "ADMIN", allowableValues = {"USER", "ADMIN"}, required = true)
    @NotNull(message = "역할은 필수입니다.")
    private UserRole role;
}
