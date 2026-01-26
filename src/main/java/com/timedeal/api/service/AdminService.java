package com.timedeal.api.service;

import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 전용 비즈니스 로직을 처리하는 Service 클래스
 * 
 * @Service:
 * - Spring의 서비스 레이어를 나타내는 어노테이션
 * - 비즈니스 로직을 담당하는 클래스임을 명시
 * 
 * @Transactional(readOnly = true):
 * - 클래스 레벨에서 기본적으로 읽기 전용 트랜잭션 설정
 * - SELECT 쿼리 최적화 (변경 감지 비활성화)
 * - 개별 메서드에서 @Transactional을 사용하면 해당 설정이 우선 적용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final OrderRepository orderRepository;
    private final ItemService itemService;
    private final UserService userService;

    /**
     * 전체 주문 목록 조회 (관리자 전용)
     * 
     * @return List<OrderResponse> (모든 주문 목록)
     */
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::new)
                .toList();
    }

    /**
     * 상품 수정 (관리자 전용)
     * 
     * @param id: 수정할 상품 ID
     * @param request: 수정할 상품 정보
     * @return ItemResponse (수정된 상품 정보)
     */
    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        return itemService.updateItem(id, request);
    }

    /**
     * 상품 삭제 (관리자 전용)
     * 
     * @param id: 삭제할 상품 ID
     */
    @Transactional
    public void deleteItem(Long id) {
        itemService.deleteItem(id);
    }

    /**
     * 사용자 역할 변경 (관리자 전용)
     * 
     * @param userId: 역할을 변경할 사용자 ID
     * @param role: 변경할 역할 (USER, ADMIN)
     * @return UserResponse (변경된 사용자 정보)
     */
    @Transactional
    public UserResponse changeUserRole(Long userId, UserRole role) {
        User user = userService.findById(userId);
        user.changeRole(role);
        // User는 @Transactional로 인해 자동으로 저장됨 (더티 체킹)
        return new UserResponse(user);
    }

    /**
     * 전체 사용자 목록 조회 (관리자 전용)
     * 
     * @return List<UserResponse> (모든 사용자 목록)
     */
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }
}
