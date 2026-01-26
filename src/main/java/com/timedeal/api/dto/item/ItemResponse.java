package com.timedeal.api.dto.item;

import com.timedeal.api.domain.item.Item;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ItemResponse {
    
    private Long id;
    private String name;
    private BigDecimal price;
    private LocalDateTime openTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public ItemResponse(Item item) {
        this.id = item.getId();
        this.name = item.getName();
        this.price = item.getPrice();
        this.openTime = item.getOpenTime();
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
    }
}
