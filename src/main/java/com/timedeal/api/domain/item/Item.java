package com.timedeal.api.domain.item;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime openTime;

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
    public Item(String name, BigDecimal price, LocalDateTime openTime) {
        this.name = name;
        this.price = price;
        this.openTime = openTime;
    }
    
    /**
     * 테스트 전용 메서드: 오픈 시간 설정
     * 
     * 주의: 이 메서드는 테스트 코드에서만 사용됩니다.
     * 프로덕션 코드에서는 사용하지 않아야 합니다.
     * 
     * @param openTime: 설정할 오픈 시간
     */
    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
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
