package com.timedeal.api.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "주문 생성 요청")
@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {
    
    @Schema(description = "상품 ID", example = "1", required = true)
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long itemId;
    
    @Schema(description = "주문 수량", example = "2", required = true)
    @NotNull(message = "주문 수량은 필수입니다.")
    @Positive(message = "주문 수량은 0보다 커야 합니다.")
    private Integer quantity;
}
