package com.timedeal.api.service;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.order.OrderRequest;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.support.TestFixtures;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.lock.StockLockService;
import com.timedeal.api.infrastructure.persistence.order.OrderRepository;
import com.timedeal.api.infrastructure.persistence.stock.StockRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OrderService 단위 테스트
 * 
 * @ExtendWith(MockitoExtension.class): Mockito를 사용한 단위 테스트
 * @Mock: Mock 객체 생성 (의존성을 대체)
 * @InjectMocks: Mock 객체를 주입받을 대상 객체
 * 
 * 이 테스트는 실제 데이터베이스 없이 Service 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockLockService stockLockService;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private OrderService orderService;

    private User user;
    private Item item;
    private Stock stock;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        user = TestFixtures.user(1L);
        item = TestFixtures.itemOpened(1L);
        stock = TestFixtures.stock(item, 100, 1L);
        orderRequest = TestFixtures.orderRequest(1L, 2);
        // OrderService에 MeterRegistry·StockLockService 주입 (createOrder 내부 Timer·분산 락용)
        orderService = new OrderService(
                orderRepository, itemService, userService, stockRepository, meterRegistry, stockLockService);
    }

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // given: Mock 객체의 동작 정의
        when(userService.findById(1L)).thenReturn(user);
        when(itemService.findById(1L)).thenReturn(item);
        when(stockRepository.findByItemIdWithLock(1L)).thenReturn(Optional.of(stock));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            // 저장된 주문의 ID를 설정 (실제로는 DB가 해주지만 테스트에서는 수동 설정)
            Order savedOrder = Order.builder()
                    .user(order.getUser())
                    .item(order.getItem())
                    .status(order.getStatus())
                    .quantity(order.getQuantity())
                    .build();
            savedOrder.setId(1L);
            return savedOrder;
        });

        // when: 주문 생성
        OrderResponse response = orderService.createOrder(1L, orderRequest);

        // then: 검증
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(response.getQuantity()).isEqualTo(2);
        
        // 재고가 차감되었는지 확인
        verify(stockRepository, times(1)).findByItemIdWithLock(1L);
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 시 비관적 락(findByItemIdWithLock) 사용, 일반 조회(findByItemId) 미사용")
    void createOrder_비관적_락_메서드_사용_검증() {
        when(userService.findById(1L)).thenReturn(user);
        when(itemService.findById(1L)).thenReturn(item);
        when(stockRepository.findByItemIdWithLock(1L)).thenReturn(Optional.of(stock));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            Order saved = Order.builder()
                    .user(o.getUser())
                    .item(o.getItem())
                    .status(o.getStatus())
                    .quantity(o.getQuantity())
                    .build();
            saved.setId(1L);
            return saved;
        });

        orderService.createOrder(1L, orderRequest);

        // 비관적 락 메서드가 호출되어야 함
        verify(stockRepository, times(1)).findByItemIdWithLock(1L);
        // 일반 조회는 주문 생성 경로에서 호출되지 않음 (취소 시에만 사용)
        verify(stockRepository, never()).findByItemId(anyLong());
    }

    @Test
    @DisplayName("주문 단건 조회 성공")
    void getOrder_Success() {
        Order order = TestFixtures.order(user, item, 2, OrderStatus.ORDERED, 1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.ORDERED);
    }

    @Test
    @DisplayName("주문 단건 조회 실패 - 없음")
    void getOrder_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("사용자별 주문 목록 조회 성공(페이징)")
    void getUserOrders_Success() {
        Order order = TestFixtures.order(user, item, 2, OrderStatus.ORDERED, 1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findByUserId(1L, pageable)).thenReturn(orderPage);

        Page<OrderResponse> result = orderService.getUserOrders(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("타임딜 오픈 전 주문 시도 - 실패")
    void createOrder_BeforeTimeDealOpens_Fail() {
        Item notOpenedItem = TestFixtures.item(1L, LocalDateTime.now().plusHours(1));
        when(userService.findById(1L)).thenReturn(user);
        when(itemService.findById(1L)).thenReturn(notOpenedItem);

        // when & then: 타임딜 오픈 전이므로 예외 발생
        assertThatThrownBy(() -> orderService.createOrder(1L, orderRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.TIMEDEAL_NOT_OPENED.getMessage());
    }

    @Test
    @DisplayName("재고 부족 시 주문 실패")
    void createOrder_InsufficientStock_Fail() {
        Stock lowStock = TestFixtures.stock(item, 1, 1L);

        when(userService.findById(1L)).thenReturn(user);
        when(itemService.findById(1L)).thenReturn(item);
        when(stockRepository.findByItemIdWithLock(1L)).thenReturn(Optional.of(lowStock));

        // when & then: 재고 부족으로 예외 발생
        assertThatThrownBy(() -> orderService.createOrder(1L, orderRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("주문 취소 성공 - 재고 복구")
    void cancelOrder_Success_StockRestored() {
        Order order = TestFixtures.order(user, item, 5, OrderStatus.ORDERED, 1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(stockRepository.findByItemId(1L)).thenReturn(Optional.of(stock));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when: 주문 취소
        OrderResponse response = orderService.cancelOrder(1L);

        // then: 검증
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELED);
        
        // 재고가 복구되었는지 확인
        verify(stockRepository, times(1)).findByItemId(1L);
        verify(stockRepository, times(1)).save(any(Stock.class));
    }

    @Test
    @DisplayName("이미 취소된 주문 취소 시도 - 실패")
    void cancelOrder_AlreadyCanceled_Fail() {
        Order canceledOrder = TestFixtures.order(user, item, 5, OrderStatus.CANCELED, 1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(canceledOrder));

        // when & then: 이미 취소된 주문이므로 예외 발생
        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_ALREADY_CANCELED.getMessage());
    }
}
