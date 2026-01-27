package com.timedeal.api.dto.order;

import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "주문 정보 응답")
@Getter
public class OrderResponse {
    
    @Schema(description = "주문 ID", example = "1")
    private Long id;
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "상품 ID", example = "1")
    private Long itemId;
    
    @Schema(description = "상품명", example = "타임딜 상품 1")
    private String itemName;
    
    @Schema(description = "주문 상태", example = "ORDERED", allowableValues = {"ORDERED", "CANCELED"})
    private OrderStatus status;
    
    @Schema(description = "주문 수량", example = "2")
    private Integer quantity;
    
    @Schema(description = "생성 일시", example = "2026-01-27T08:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시", example = "2026-01-27T08:00:00")
    private LocalDateTime updatedAt;
    
    public OrderResponse(Order order) {
        this.id = order.getId();
        this.userId = order.getUser().getId();
        this.itemId = order.getItem().getId();
        this.itemName = order.getItem().getName();
        this.status = order.getStatus();
        this.quantity = order.getQuantity();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
    }
}
