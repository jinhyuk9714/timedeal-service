package com.timedeal.api.controller;

import com.timedeal.api.common.ApiPaths;
import com.timedeal.api.dto.order.OrderRequest;
import com.timedeal.api.dto.order.OrderResponse;
import com.timedeal.api.service.OrderService;
import com.timedeal.api.support.TestFixtures;
import com.timedeal.api.support.WebTestSupport;
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
    private MockMvc mockMvc;

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private com.timedeal.api.infrastructure.security.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.timedeal.api.infrastructure.security.TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private com.timedeal.api.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        objectMapper = WebTestSupport.objectMapper();
    }

    @Test
    @DisplayName("주문 생성 - 인증 없음 시 401")
    void createOrder_Unauthorized() throws Exception {
        OrderRequest request = TestFixtures.orderRequest(1L, 2);

        mockMvc.perform(post(ApiPaths.ORDERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("주문 생성 - 유효성 검증 실패 시 400")
    void createOrder_ValidationFailed() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setQuantity(2);

        mockMvc.perform(post(ApiPaths.ORDERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 단건 조회 성공")
    void getOrder_Success() throws Exception {
        var user = TestFixtures.user(1L);
        var item = TestFixtures.itemOpened(1L);
        var order = TestFixtures.order(user, item, 2, com.timedeal.api.domain.order.OrderStatus.ORDERED, 1L);
        OrderResponse response = new OrderResponse(order);

        when(orderService.getOrder(eq(1L))).thenReturn(response);

        mockMvc.perform(get(ApiPaths.ORDERS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    @DisplayName("내 주문 목록 조회 - 인증 없음 시 401")
    void getMyOrders_Unauthorized() throws Exception {
        mockMvc.perform(get(ApiPaths.ORDERS + "/my-orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() throws Exception {
        var user = TestFixtures.user(1L);
        var item = TestFixtures.itemOpened(1L);
        var order = TestFixtures.order(user, item, 2, com.timedeal.api.domain.order.OrderStatus.CANCELED, 1L);
        OrderResponse response = new OrderResponse(order);

        when(orderService.cancelOrder(eq(1L))).thenReturn(response);

        mockMvc.perform(patch(ApiPaths.ORDERS + "/1/cancel"))
                .andExpect(status().isOk());
    }
}
