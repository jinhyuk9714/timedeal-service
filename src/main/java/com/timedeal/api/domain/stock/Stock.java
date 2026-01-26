package com.timedeal.api.domain.stock;

import com.timedeal.api.domain.item.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private Item item;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Stock(Item item, Integer quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public void decrease(int quantity) {
        if (this.quantity < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        this.quantity -= quantity;
    }

    public void increase(int quantity) {
        this.quantity += quantity;
    }
    
    /**
     * 테스트 전용 메서드: 재고 수량 설정
     * 
     * 주의: 이 메서드는 테스트 코드에서만 사용됩니다.
     * 프로덕션 코드에서는 사용하지 않아야 합니다.
     * 
     * @param quantity: 설정할 재고 수량
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    /**
     * 테스트 전용 메서드: ID 설정
     * 
     * 주의: 이 메서드는 테스트 코드에서만 사용됩니다.
     * 프로덕션 코드에서는 사용하지 않아야 합니다.
     * 
     * @param id: 설정할 ID
     */
    public void setId(Long id) {
        this.id = id;
    }
}
