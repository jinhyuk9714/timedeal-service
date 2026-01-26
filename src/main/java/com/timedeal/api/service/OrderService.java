package com.timedeal.api.service;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.order.OrderRequest;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.order.OrderRepository;
import com.timedeal.api.infrastructure.persistence.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문(Order) 관련 비즈니스 로직을 처리하는 Service 클래스
 * 
 * @Service:
 * - Spring의 서비스 레이어를 나타내는 어노테이션
 * - @Component의 특수화된 형태
 * - 비즈니스 로직을 담당하는 클래스임을 명시
 * - Spring이 자동으로 빈으로 등록하여 관리
 * 
 * @Transactional(readOnly = true):
 * - 클래스 레벨에서 기본적으로 읽기 전용 트랜잭션 설정
 * - SELECT 쿼리 최적화 (변경 감지 비활성화)
 * - 개별 메서드에서 @Transactional을 사용하면 해당 설정이 우선 적용
 * 
 * 트랜잭션(Transaction):
 * - 데이터베이스 작업의 원자성 보장
 * - 여러 작업이 모두 성공하거나 모두 실패해야 함 (All or Nothing)
 * - 예: 주문 생성 시 재고 차감과 주문 저장이 모두 성공해야 함
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ItemService itemService;
    private final UserService userService;
    private final StockRepository stockRepository;
    
    /**
     * 주문 생성 메서드
     * 
     * @Transactional:
     * - 이 메서드를 하나의 트랜잭션으로 실행
     * - 메서드 내에서 예외 발생 시 모든 DB 변경사항이 롤백됨
     * - 클래스 레벨의 readOnly=true를 오버라이드하여 읽기/쓰기 트랜잭션으로 변경
     * 
     * 트랜잭션 전파(Propagation):
     * - 기본값: REQUIRED (기존 트랜잭션이 있으면 참여, 없으면 새로 생성)
     * - 이 메서드가 호출되면 새로운 트랜잭션이 시작됨
     * 
     * 비관적 락(Pessimistic Lock):
     * - findByItemIdWithLock: SELECT ... FOR UPDATE 쿼리 실행
     * - 다른 트랜잭션이 해당 재고를 수정하지 못하도록 락을 걸음
     * - 동시성 문제 해결 (여러 사용자가 동시에 주문해도 재고가 정확하게 차감됨)
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        // 1. 사용자 조회
        // - 존재하지 않는 사용자면 예외 발생
        User user = userService.findById(userId);
        
        // 2. 상품 조회
        // - 존재하지 않는 상품이면 예외 발생
        Item item = itemService.findById(request.getItemId());
        
        // 3. 타임딜 오픈 시간 체크
        // - 비즈니스 규칙: 타임딜 오픈 시간이 지나야 주문 가능
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getOpenTime())) {
            throw new BusinessException(ErrorCode.TIMEDEAL_NOT_OPENED);
        }
        
        // 4. 재고 확인 및 차감 (비관적 락 사용)
        // - findByItemIdWithLock: SELECT FOR UPDATE로 락을 걸고 조회
        // - 동시에 여러 주문이 들어와도 재고가 정확하게 관리됨
        Stock stock = stockRepository.findByItemIdWithLock(request.getItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        
        // 5. 재고 부족 체크
        if (stock.getQuantity() < request.getQuantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        
        // 6. 재고 차감
        // - 도메인 객체의 메서드를 통해 비즈니스 로직 실행
        // - 재고가 부족하면 도메인 객체에서 예외 발생
        stock.decrease(request.getQuantity());
        stockRepository.save(stock); // 변경사항 저장
        
        // 7. 주문 생성
        // - Builder 패턴으로 객체 생성 (가독성 향상)
        Order order = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.ORDERED)
                .quantity(request.getQuantity())
                .build();
        
        // 8. 주문 저장 및 응답 반환
        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(savedOrder);
    }
    
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return new OrderResponse(order);
    }
    
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::new)
                .toList();
    }
    
    /**
     * 주문 취소 메서드
     * 
     * @Transactional:
     * - 주문 취소와 재고 복구를 하나의 트랜잭션으로 처리
     * - 둘 중 하나라도 실패하면 전체 롤백
     * 
     * 주문 취소 시 재고 복구:
     * - 취소된 주문의 수량만큼 재고를 다시 증가시킴
     * - 비즈니스 규칙: 주문 취소 시 재고가 원래대로 돌아가야 함
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 2. 주문 취소 시도
        // - 도메인 객체의 cancel() 메서드 호출
        // - 이미 취소된 주문이면 IllegalStateException 발생
        try {
            order.cancel();
        } catch (IllegalStateException e) {
            // 3. 비즈니스 예외로 변환
            // - 도메인 예외를 애플리케이션 예외로 변환하여 일관성 유지
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELED);
        }
        
        // 4. 재고 복구
        // - 취소된 주문의 수량만큼 재고 증가
        Stock stock = stockRepository.findByItemId(order.getItem().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        stock.increase(order.getQuantity());
        stockRepository.save(stock);
        
        // 5. 변경된 주문 저장 및 응답 반환
        Order savedOrder = orderRepository.save(order);
        return new OrderResponse(savedOrder);
    }
}
