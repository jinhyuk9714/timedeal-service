package com.timedeal.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.order.OrderStatus;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.item.ItemRequest;
import com.timedeal.api.dto.order.OrderRequest;
import com.timedeal.api.dto.user.UserRequest;
import com.timedeal.api.exception.ErrorCode;
import com.timedeal.api.infrastructure.persistence.item.ItemRepository;
import com.timedeal.api.infrastructure.persistence.order.OrderRepository;
import com.timedeal.api.infrastructure.persistence.stock.StockRepository;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 타임딜 서비스 통합 테스트
 * 
 * @SpringBootTest: 전체 Spring Boot 애플리케이션 컨텍스트를 로드
 * @AutoConfigureMockMvc: MockMvc를 자동 설정하여 HTTP 요청 테스트 가능
 * @Testcontainers: Docker 컨테이너를 사용한 통합 테스트
 * @Transactional: 각 테스트 후 롤백하여 데이터 격리
 * 
 * 이 테스트는 실제 데이터베이스와 통신하며 전체 플로우를 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class TimeDealIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_timedeal")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    private Long userId;
    private Long itemId;

    /**
     * 각 테스트 전에 실행되는 메서드
     * 공통 테스트 데이터를 준비합니다.
     */
    @BeforeEach
    void setUp() {
        // ObjectMapper 직접 생성 (LocalDateTime 지원)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // 사용자 생성
        User user = User.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트 사용자")
                .build();
        User savedUser = userRepository.save(user);
        userId = savedUser.getId();

        // 상품 생성 (1시간 후 오픈)
        Item item = Item.builder()
                .name("타임딜 상품")
                .price(new BigDecimal("10000"))
                .openTime(LocalDateTime.now().plusHours(1))
                .build();
        Item savedItem = itemRepository.save(item);
        itemId = savedItem.getId();

        // 재고 생성
        Stock stock = Stock.builder()
                .item(savedItem)
                .quantity(100)
                .build();
        stockRepository.save(stock);
    }

    @Test
    @DisplayName("전체 플로우 테스트: 사용자 생성 → 상품 등록 → 주문 생성")
    void fullFlowTest() throws Exception {
        // 1. 사용자 생성
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail("newuser@test.com");
        userRequest.setPassword("password");
        userRequest.setName("새 사용자");

        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long newUserId = objectMapper.readTree(userResponse).get("id").asLong();

        // 2. 상품 등록
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setName("새 타임딜 상품");
        itemRequest.setPrice(new BigDecimal("20000"));
        itemRequest.setOpenTime(LocalDateTime.now().plusHours(2));
        itemRequest.setStockQuantity(50);

        String itemResponse = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("새 타임딜 상품"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long newItemId = objectMapper.readTree(itemResponse).get("id").asLong();

        // 3. 주문 생성 (타임딜 시간이 지난 상품으로 변경)
        Item item = itemRepository.findById(newItemId).orElseThrow();
        item.setOpenTime(LocalDateTime.now().minusHours(1)); // 이미 오픈된 상품
        itemRepository.save(item);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItemId(newItemId);
        orderRequest.setQuantity(2);

        mockMvc.perform(post("/api/orders/users/{userId}", newUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.quantity").value(2));

        // 4. 재고 확인 (2개 차감되었는지 확인)
        Stock stock = stockRepository.findByItemId(newItemId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(48); // 50 - 2 = 48
    }

    @Test
    @DisplayName("타임딜 오픈 전 주문 시도 - 실패")
    void orderBeforeTimeDealOpens_Fail() throws Exception {
        // given: 아직 오픈되지 않은 상품
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItemId(itemId);
        orderRequest.setQuantity(1);

        // when & then: 타임딜 오픈 전이므로 실패해야 함
        mockMvc.perform(post("/api/orders/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.TIMEDEAL_NOT_OPENED.getMessage()));
    }

    @Test
    @DisplayName("재고 부족 시 주문 실패")
    void orderWithInsufficientStock_Fail() throws Exception {
        // given: 상품을 오픈 상태로 변경
        Item item = itemRepository.findById(itemId).orElseThrow();
        item.setOpenTime(LocalDateTime.now().minusHours(1));
        itemRepository.save(item);

        // 재고를 1개로 제한
        Stock stock = stockRepository.findByItemId(itemId).orElseThrow();
        stock.setQuantity(1);
        stockRepository.save(stock);

        // when: 2개 주문 시도
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItemId(itemId);
        orderRequest.setQuantity(2);

        // then: 재고 부족으로 실패
        mockMvc.perform(post("/api/orders/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.INSUFFICIENT_STOCK.getMessage()));
    }

    @Test
    @DisplayName("주문 취소 성공 - 재고 복구 확인")
    void cancelOrder_Success_StockRestored() throws Exception {
        // given: 상품을 오픈 상태로 변경
        Item item = itemRepository.findById(itemId).orElseThrow();
        item.setOpenTime(LocalDateTime.now().minusHours(1));
        itemRepository.save(item);

        // 주문 생성
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItemId(itemId);
        orderRequest.setQuantity(5);

        String orderResponse = mockMvc.perform(post("/api/orders/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("id").asLong();

        // 재고 확인 (100 - 5 = 95)
        Stock stockBefore = stockRepository.findByItemId(itemId).orElseThrow();
        assertThat(stockBefore.getQuantity()).isEqualTo(95);

        // when: 주문 취소
        mockMvc.perform(patch("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // then: 재고가 복구되었는지 확인 (95 + 5 = 100)
        Stock stockAfter = stockRepository.findByItemId(itemId).orElseThrow();
        assertThat(stockAfter.getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("사용자별 주문 목록 조회")
    void getUserOrders_Success() throws Exception {
        // given: 상품을 오픈 상태로 변경
        Item item = itemRepository.findById(itemId).orElseThrow();
        item.setOpenTime(LocalDateTime.now().minusHours(1));
        itemRepository.save(item);

        // 주문 2개 생성
        OrderRequest orderRequest1 = new OrderRequest();
        orderRequest1.setItemId(itemId);
        orderRequest1.setQuantity(1);

        OrderRequest orderRequest2 = new OrderRequest();
        orderRequest2.setItemId(itemId);
        orderRequest2.setQuantity(2);

        mockMvc.perform(post("/api/orders/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest2)))
                .andExpect(status().isCreated());

        // when & then: 사용자별 주문 목록 조회
        mockMvc.perform(get("/api/orders/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andDo(print());
    }

    @Test
    @DisplayName("중복 이메일로 사용자 생성 실패")
    void createUser_DuplicateEmail_Fail() throws Exception {
        // given: 이미 존재하는 이메일
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail("test@test.com"); // setUp에서 생성한 이메일
        userRequest.setPassword("password");
        userRequest.setName("중복 사용자");

        // when & then: 중복 이메일로 인해 실패
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_ALREADY_EXISTS.getMessage()));
    }
}
