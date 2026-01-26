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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Item item;
    private Stock stock;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        user = User.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트 사용자")
                .build();
        user.setId(1L);

        item = Item.builder()
                .name("타임딜 상품")
                .price(new BigDecimal("10000"))
                .openTime(LocalDateTime.now().minusHours(1)) // 이미 오픈된 상품
                .build();
        item.setId(1L);

        stock = Stock.builder()
                .item(item)
                .quantity(100)
                .build();
        stock.setId(1L);

        orderRequest = new OrderRequest();
        orderRequest.setItemId(1L);
        orderRequest.setQuantity(2);
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
    @DisplayName("타임딜 오픈 전 주문 시도 - 실패")
    void createOrder_BeforeTimeDealOpens_Fail() {
        // given: 아직 오픈되지 않은 상품
        Item notOpenedItem = Item.builder()
                .name("타임딜 상품")
                .price(new BigDecimal("10000"))
                .openTime(LocalDateTime.now().plusHours(1)) // 아직 오픈 안됨
                .build();
        notOpenedItem.setId(1L);

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
        // given: 재고가 부족한 상황
        Stock lowStock = Stock.builder()
                .item(item)
                .quantity(1) // 재고 1개만 있음
                .build();
        lowStock.setId(1L);

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
        // given: 주문 생성
        Order order = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.ORDERED)
                .quantity(5)
                .build();
        order.setId(1L);

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
        // given: 이미 취소된 주문
        Order canceledOrder = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.CANCELED)
                .quantity(5)
                .build();
        canceledOrder.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(canceledOrder));

        // when & then: 이미 취소된 주문이므로 예외 발생
        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ORDER_ALREADY_CANCELED.getMessage());
    }
}
