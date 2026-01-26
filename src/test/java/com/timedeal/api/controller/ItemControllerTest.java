package com.timedeal.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.timedeal.api.infrastructure.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ItemController 통합 테스트
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
 */
@WebMvcTest(controllers = ItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청/응답을 시뮬레이션하는 객체

    private ObjectMapper objectMapper; // JSON 변환을 위한 객체

    @MockitoBean
    private ItemService itemService; // Service는 Mock으로 대체
    
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
    @DisplayName("상품 등록 성공 테스트")
    void createItem_Success() throws Exception {
        // given: 테스트 데이터 준비
        ItemRequest request = new ItemRequest();
        request.setName("테스트 상품");
        request.setPrice(new BigDecimal("10000"));
        request.setOpenTime(LocalDateTime.now().plusHours(1));
        request.setStockQuantity(100);

        // when & then: HTTP 요청 실행 및 검증
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()); // 201 상태 코드 확인
    }

    @Test
    @DisplayName("상품 등록 실패 - 유효성 검증 실패")
    void createItem_ValidationFailed() throws Exception {
        // given: 잘못된 데이터 (상품명이 null)
        ItemRequest request = new ItemRequest();
        request.setPrice(new BigDecimal("10000"));
        // name이 null이므로 유효성 검증 실패 예상

        // when & then
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400 상태 코드 확인
    }
}
