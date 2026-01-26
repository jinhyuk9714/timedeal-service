package com.timedeal.api.service;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.infrastructure.persistence.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * AdminService 단위 테스트
 * 
 * @ExtendWith(MockitoExtension.class): Mockito를 사용한 단위 테스트
 * @Mock: Mock 객체 생성 (의존성을 대체)
 * @InjectMocks: Mock 객체를 주입받을 대상 객체
 * 
 * 이 테스트는 실제 데이터베이스 없이 Service 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private Item item;
    private Order order;
    private ItemRequest itemRequest;
    private ItemResponse itemResponse;
    private UserResponse userResponse;

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
                .openTime(LocalDateTime.now().plusHours(1))
                .build();
        item.setId(1L);

        order = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.ORDERED)
                .quantity(2)
                .build();
        order.setId(1L);

        itemRequest = new ItemRequest();
        itemRequest.setName("타임딜 상품");
        itemRequest.setPrice(new BigDecimal("10000"));
        itemRequest.setOpenTime(LocalDateTime.now().plusHours(1));
        itemRequest.setStockQuantity(100);

        itemResponse = new ItemResponse(item, null);
        userResponse = new UserResponse(user);
    }

    @Test
    @DisplayName("전체 주문 목록 조회 성공")
    void getAllOrders_Success() {
        // given: 여러 주문 데이터
        Order order1 = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.ORDERED)
                .quantity(2)
                .build();
        order1.setId(1L);

        Order order2 = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.ORDERED)
                .quantity(3)
                .build();
        order2.setId(2L);

        List<Order> orders = Arrays.asList(order1, order2);

        when(orderRepository.findAll()).thenReturn(orders);

        // when: 전체 주문 목록 조회
        List<OrderResponse> responses = adminService.getAllOrders();

        // then: 검증
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateItem_Success() {
        // given
        when(itemService.updateItem(1L, itemRequest)).thenReturn(itemResponse);

        // when: 상품 수정
        ItemResponse response = adminService.updateItem(1L, itemRequest);

        // then: 검증
        assertThat(response).isNotNull();
        verify(itemService, times(1)).updateItem(1L, itemRequest);
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteItem_Success() {
        // given
        doNothing().when(itemService).deleteItem(1L);

        // when: 상품 삭제
        adminService.deleteItem(1L);

        // then: 검증
        verify(itemService, times(1)).deleteItem(1L);
    }

    @Test
    @DisplayName("사용자 역할 변경 성공")
    void changeUserRole_Success() {
        // given
        when(userService.findById(1L)).thenReturn(user);
        // User는 @Transactional로 인해 자동 저장되므로 별도 save 호출 없음

        // when: 사용자 역할 변경
        UserResponse response = adminService.changeUserRole(1L, UserRole.ADMIN);

        // then: 검증
        assertThat(response).isNotNull();
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userService, times(1)).findById(1L);
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 성공")
    void getAllUsers_Success() {
        // given
        List<UserResponse> userResponses = Arrays.asList(userResponse);
        when(userService.getAllUsers()).thenReturn(userResponses);

        // when: 전체 사용자 목록 조회
        List<UserResponse> responses = adminService.getAllUsers();

        // then: 검증
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        verify(userService, times(1)).getAllUsers();
    }
}
