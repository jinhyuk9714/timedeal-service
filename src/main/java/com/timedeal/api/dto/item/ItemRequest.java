package com.timedeal.api.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "상품 등록/수정 요청")
@Getter
@Setter
@NoArgsConstructor
public class ItemRequest {
    
    @Schema(description = "상품명", example = "타임딜 상품 1", required = true)
    @NotBlank(message = "상품명은 필수입니다.")
    private String name;
    
    @Schema(description = "가격", example = "99000", required = true)
    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private BigDecimal price;
    
    @Schema(description = "타임딜 오픈 시간", example = "2026-01-27T10:00:00", required = true)
    @NotNull(message = "오픈 시간은 필수입니다.")
    private LocalDateTime openTime;
    
    @Schema(description = "재고 수량", example = "100", required = true)
    @NotNull(message = "재고 수량은 필수입니다.")
    @Positive(message = "재고 수량은 0보다 커야 합니다.")
    private Integer stockQuantity;
}
