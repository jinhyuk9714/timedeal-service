package com.timedeal.api.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role; // 사용자 역할 (USER, ADMIN)

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
    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = UserRole.USER; // 기본값: 일반 사용자
    }
    
    /**
     * 관리자로 역할 변경
     * 
     * @param role: 변경할 역할
     */
    public void changeRole(UserRole role) {
        this.role = role;
    }
    
    /**
     * 관리자 여부 확인
     * 
     * @return 관리자이면 true, 아니면 false
     */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
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
