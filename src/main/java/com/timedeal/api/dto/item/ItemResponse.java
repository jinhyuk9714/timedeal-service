package com.timedeal.api.dto.item;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.stock.Stock;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ItemResponse {
    
    private Long id;
    private String name;
    private BigDecimal price;
    private LocalDateTime openTime;
    private Integer stockQuantity; // 남은 재고 수량
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Item 엔티티로부터 ItemResponse 생성
     * 
     * @param item: 상품 엔티티
     */
    public ItemResponse(Item item) {
        this.id = item.getId();
        this.name = item.getName();
        this.price = item.getPrice();
        this.openTime = item.getOpenTime();
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
        // 재고 정보는 별도로 설정해야 함 (Item과 Stock은 1:1 관계이지만 LAZY 로딩)
        this.stockQuantity = null;
    }
    
    /**
     * Item과 Stock 엔티티로부터 ItemResponse 생성
     * 
     * @param item: 상품 엔티티
     * @param stock: 재고 엔티티
     */
    public ItemResponse(Item item, Stock stock) {
        this.id = item.getId();
        this.name = item.getName();
        this.price = item.getPrice();
        this.openTime = item.getOpenTime();
        this.stockQuantity = stock != null ? stock.getQuantity() : null;
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
    }
}
