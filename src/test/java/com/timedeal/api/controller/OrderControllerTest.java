package com.timedeal.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.order.Order;
import com.timedeal.api.domain.order.OrderStatus;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.order.OrderRequest;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.infrastructure.security.JwtTokenProvider;
import com.timedeal.api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
 * OrderController 통합 테스트
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
 * - @AuthenticationPrincipal은 null이 될 수 있으므로 Controller에서 null 체크 필요
 */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청/응답을 시뮬레이션하는 객체

    private ObjectMapper objectMapper; // JSON 변환을 위한 객체

    @MockitoBean
    private OrderService orderService; // Service는 Mock으로 대체

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider; // Security 관련 빈 Mock 처리

    @MockitoBean
    private com.timedeal.api.infrastructure.security.TokenBlacklistService tokenBlacklistService; // Redis 관련 빈 Mock 처리

    @MockitoBean
    private com.timedeal.api.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 필터 Mock 처리

    @BeforeEach
    void setUp() {
        // ObjectMapper 직접 생성
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("주문 생성 성공 - 인증 없음")
    void createOrder_Unauthorized() throws Exception {
        // given: 테스트 데이터
        OrderRequest request = new OrderRequest();
        request.setItemId(1L);
        request.setQuantity(2);

        // @AuthenticationPrincipal이 null이므로 Controller에서 401 반환
        // 실제로는 SecurityContext에 인증 정보가 있어야 함
        // 테스트에서는 Service 호출 전에 401이 반환됨

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 401 상태 코드 (인증 없음)
    }

    @Test
    @DisplayName("주문 생성 실패 - 유효성 검증 실패")
    void createOrder_ValidationFailed() throws Exception {
        // given: 잘못된 데이터 (itemId가 null)
        OrderRequest request = new OrderRequest();
        request.setQuantity(2);
        // itemId가 null이므로 유효성 검증 실패 예상

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400 상태 코드
    }

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_Success() throws Exception {
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

        Order order = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.ORDERED)
                .quantity(2)
                .build();
        order.setId(1L);

        OrderResponse response = new OrderResponse(order);

        when(orderService.getOrder(eq(1L))).thenReturn(response);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자별 주문 목록 조회 - 인증 없음")
    void getMyOrders_Unauthorized() throws Exception {
        // given: @AuthenticationPrincipal이 null이므로 401 반환

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(get("/api/orders/my-orders"))
                .andExpect(status().isUnauthorized()); // 401 상태 코드
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() throws Exception {
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

        Order order = Order.builder()
                .user(user)
                .item(item)
                .status(OrderStatus.CANCELED)
                .quantity(2)
                .build();
        order.setId(1L);

        OrderResponse response = new OrderResponse(order);

        when(orderService.cancelOrder(eq(1L))).thenReturn(response);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(patch("/api/orders/1/cancel"))
                .andExpect(status().isOk());
    }
}
