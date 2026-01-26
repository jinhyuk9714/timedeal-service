package com.timedeal.api.dto.order;

import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderResponse {
    
    private Long id;
    private Long userId;
    private Long itemId;
    private String itemName;
    private OrderStatus status;
    private Integer quantity;
    private LocalDateTime createdAt;
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
