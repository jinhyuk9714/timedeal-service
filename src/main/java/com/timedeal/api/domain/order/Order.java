package com.timedeal.api.domain.order;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_created_at", columnList = "user_id, createdAt DESC"),
                @Index(name = "idx_orders_item_id", columnList = "item_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

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
    public Order(User user, Item item, OrderStatus status, Integer quantity) {
        this.user = user;
        this.item = item;
        this.status = status;
        this.quantity = quantity;
    }

    public void cancel() {
        if (this.status == OrderStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        this.status = OrderStatus.CANCELED;
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
