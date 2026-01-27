package com.timedeal.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.domain.user.UserRole;
import com.timedeal.api.common.ApiPaths;
import com.timedeal.api.dto.admin.ChangeRoleRequest;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.dto.user.UserResponse;
import com.timedeal.api.infrastructure.security.JwtTokenProvider;
import com.timedeal.api.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController 통합 테스트
 * 
 * @WebMvcTest: 웹 레이어만 테스트하는 어노테이션
 * - Controller, Filter, Interceptor 등 웹 관련 빈만 로드
 * - Service는 Mock으로 대체 (@MockitoBean 사용)
 * - 빠른 테스트 실행 가능
 * 
 * @ActiveProfiles("test"):
 * - test 프로파일 활성화하여 TestSecurityConfig 사용
 * 
 * @AutoConfigureMockMvc(addFilters = false):
 * - Security 필터를 제외하여 인증 없이 API를 테스트
 * - 실제로는 @PreAuthorize가 동작하지 않지만, Service 로직은 테스트 가능
 */
@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청/응답을 시뮬레이션하는 객체

    private ObjectMapper objectMapper; // JSON 변환을 위한 객체

    @MockitoBean
    private AdminService adminService; // Service는 Mock으로 대체

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider; // Security 관련 빈 Mock 처리

    @MockitoBean
    private com.timedeal.api.infrastructure.security.TokenBlacklistService tokenBlacklistService; // Redis 관련 빈 Mock 처리

    @MockitoBean
    private com.timedeal.api.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 필터 Mock 처리

    @BeforeEach
    void setUp() {
        // ObjectMapper 직접 생성 (LocalDateTime 지원)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("전체 주문 목록 조회 성공 (페이징)")
    void getAllOrders_Success() throws Exception {
        // given: Mock 데이터 - 실제 Order 객체 생성
        User user = User.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트 사용자")
                .build();
        user.setId(1L);

        Item item = Item.builder()
                .name("타임딜 상품")
                .price(new BigDecimal("10000"))
                .openTime(LocalDateTime.now().plusHours(1))
                .build();
        item.setId(1L);

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

        List<OrderResponse> orders = Arrays.asList(
                new OrderResponse(order1),
                new OrderResponse(order2)
        );
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderResponse> orderPage = new PageImpl<>(orders, pageable, orders.size());

        when(adminService.getAllOrders(any(Pageable.class))).thenReturn(orderPage);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(get(ApiPaths.ADMIN + "/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateItem_Success() throws Exception {
        // given: 테스트 데이터 - 실제 Item, Stock 객체 생성
        Item item = Item.builder()
                .name("수정된 상품명")
                .price(new BigDecimal("20000"))
                .openTime(LocalDateTime.now().plusHours(2))
                .build();
        item.setId(1L);

        Stock stock = Stock.builder()
                .item(item)
                .quantity(200)
                .build();
        stock.setId(1L);

        ItemRequest request = new ItemRequest();
        request.setName("수정된 상품명");
        request.setPrice(new BigDecimal("20000"));
        request.setOpenTime(LocalDateTime.now().plusHours(2));
        request.setStockQuantity(200);

        ItemResponse response = new ItemResponse(item, stock);

        when(adminService.updateItem(eq(1L), any(ItemRequest.class))).thenReturn(response);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(put(ApiPaths.ADMIN + "/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteItem_Success() throws Exception {
        // given: Mock 설정
        // deleteItem은 void 메서드이므로 when().thenReturn() 불필요

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(delete(ApiPaths.ADMIN + "/items/1"))
                .andExpect(status().isNoContent()); // 204 상태 코드
    }

    @Test
    @DisplayName("사용자 역할 변경 성공")
    void changeUserRole_Success() throws Exception {
        // given: 테스트 데이터 - 실제 User 객체 생성
        User user = User.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트 사용자")
                .build();
        user.setId(1L);
        user.changeRole(UserRole.ADMIN);

        ChangeRoleRequest request = new ChangeRoleRequest();
        request.setRole(UserRole.ADMIN);

        UserResponse response = new UserResponse(user);

        when(adminService.changeUserRole(eq(1L), eq(UserRole.ADMIN))).thenReturn(response);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(patch(ApiPaths.ADMIN + "/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 성공 (페이징)")
    void getAllUsers_Success() throws Exception {
        // given: Mock 데이터 - 실제 User 객체 생성
        User user1 = User.builder()
                .email("user1@test.com")
                .password("password1")
                .name("사용자1")
                .build();
        user1.setId(1L);

        User user2 = User.builder()
                .email("user2@test.com")
                .password("password2")
                .name("사용자2")
                .build();
        user2.setId(2L);

        List<UserResponse> users = Arrays.asList(
                new UserResponse(user1),
                new UserResponse(user2)
        );
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserResponse> userPage = new PageImpl<>(users, pageable, users.size());

        when(adminService.getAllUsers(any(Pageable.class))).thenReturn(userPage);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(get(ApiPaths.ADMIN + "/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
