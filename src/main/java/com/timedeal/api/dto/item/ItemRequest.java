package com.timedeal.api.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ItemRequest {
    
    @NotBlank(message = "상품명은 필수입니다.")
    private String name;
    
    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private BigDecimal price;
    
    @NotNull(message = "오픈 시간은 필수입니다.")
    private LocalDateTime openTime;
    
    @NotNull(message = "재고 수량은 필수입니다.")
    @Positive(message = "재고 수량은 0보다 커야 합니다.")
    private Integer stockQuantity;
}
