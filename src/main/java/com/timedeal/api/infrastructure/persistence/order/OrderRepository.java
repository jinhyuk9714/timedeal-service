package com.timedeal.api.infrastructure.persistence.order;

import com.timedeal.api.domain.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    /**
     * 사용자 ID로 주문 목록 조회 (페이징)
     * 
     * @param userId: 사용자 ID
     * @param pageable: 페이징 정보
     * @return Page<Order> (페이징된 주문 목록)
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 사용자 ID로 주문 목록 조회 (페이징 없음, 기존 호환성 유지)
     */
    List<Order> findByUserId(Long userId);
    
    List<Order> findByItemId(Long itemId);
}
