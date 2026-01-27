package com.timedeal.api.dto.item;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.stock.Stock;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "상품 정보 응답")
@Getter
public class ItemResponse {
    
    @Schema(description = "상품 ID", example = "1")
    private Long id;
    
    @Schema(description = "상품명", example = "타임딜 상품 1")
    private String name;
    
    @Schema(description = "가격", example = "99000.00")
    private BigDecimal price;
    
    @Schema(description = "타임딜 오픈 시간", example = "2026-01-27T10:00:00")
    private LocalDateTime openTime;
    
    @Schema(description = "남은 재고 수량", example = "100")
    private Integer stockQuantity;
    
    @Schema(description = "생성 일시", example = "2026-01-27T08:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시", example = "2026-01-27T08:00:00")
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
