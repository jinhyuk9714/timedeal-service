package com.timedeal.api.controller;

import com.timedeal.api.common.ApiPaths;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.item.ItemResponse;
import com.timedeal.api.exception.BusinessException;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.service.ItemService;
import com.timedeal.api.support.TestFixtures;
import com.timedeal.api.support.WebTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
    private MockMvc mockMvc;

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @MockitoBean
    private ItemService itemService;

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
    @DisplayName("상품 등록 성공")
    void createItem_Success() throws Exception {
        ItemRequest request = TestFixtures.itemRequest("테스트 상품", new BigDecimal("10000"),
                LocalDateTime.now().plusHours(1), 100);
        // Mock 응답이 요청과 동일한 상품명을 갖도록 함 (실제 서비스 동작과 맞춤)
        com.timedeal.api.domain.item.Item requestItem = com.timedeal.api.domain.item.Item.builder()
                .name(request.getName())
                .price(request.getPrice())
                .openTime(request.getOpenTime())
                .build();
        requestItem.setId(1L);
        var stock = TestFixtures.stock(requestItem, 100, 1L);
        ItemResponse response = new ItemResponse(requestItem, stock);
        when(itemService.createItem(any(ItemRequest.class))).thenReturn(response);

        mockMvc.perform(post(ApiPaths.ITEMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("테스트 상품"))
                .andExpect(jsonPath("$.stockQuantity").value(100));
    }

    @Test
    @DisplayName("상품 등록 실패 - 유효성 검증 실패")
    void createItem_ValidationFailed() throws Exception {
        ItemRequest request = new ItemRequest();
        request.setPrice(new BigDecimal("10000"));

        mockMvc.perform(post(ApiPaths.ITEMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void getItem_Success() throws Exception {
        var item = TestFixtures.itemOpened(1L);
        var stock = TestFixtures.stock(item, 100, 1L);
        ItemResponse response = new ItemResponse(item, stock);
        when(itemService.getItem(eq(1L))).thenReturn(response);

        mockMvc.perform(get(ApiPaths.ITEMS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stockQuantity").value(100));
    }

    @Test
    @DisplayName("상품 단건 조회 실패 - 없음")
    void getItem_NotFound() throws Exception {
        when(itemService.getItem(eq(999L)))
                .thenThrow(new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        mockMvc.perform(get(ApiPaths.ITEMS + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상품 목록 조회 성공(페이징, 검색 없음)")
    void getItems_Success() throws Exception {
        var item = TestFixtures.itemOpened(1L);
        var stock = TestFixtures.stock(item, 100, 1L);
        var page = new PageImpl<>(List.of(new ItemResponse(item, stock)), PageRequest.of(0, 20), 1);
        when(itemService.getItems(any(), any())).thenReturn(page);

        mockMvc.perform(get(ApiPaths.ITEMS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("상품 목록 조회 성공 - 검색 조건(name) 적용")
    void getItems_WithSearchCondition_Success() throws Exception {
        var item = TestFixtures.itemOpened(1L);
        var stock = TestFixtures.stock(item, 100, 1L);
        var page = new PageImpl<>(List.of(new ItemResponse(item, stock)), PageRequest.of(0, 20), 1);
        when(itemService.getItems(any(), any())).thenReturn(page);

        mockMvc.perform(get(ApiPaths.ITEMS).param("name", "타임딜"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
