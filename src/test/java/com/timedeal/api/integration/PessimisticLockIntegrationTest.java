package com.timedeal.api.integration;

import com.timedeal.api.domain.item.Item;
import com.timedeal.api.domain.stock.Stock;
import com.timedeal.api.domain.user.User;
import com.timedeal.api.dto.order.OrderRequest;
import com.timedeal.api.infrastructure.persistence.item.ItemRepository;
import com.timedeal.api.infrastructure.persistence.stock.StockRepository;
import com.timedeal.api.infrastructure.persistence.user.UserRepository;
import com.timedeal.api.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비관적 락(Pessimistic Lock) 동작을 검증하는 통합 테스트.
 *
 * <p>동시에 여러 스레드가 같은 상품을 주문해도 재고가 정확히 차감되는지 검증합니다.
 * OrderService.createOrder()는 StockRepository.findByItemIdWithLock()을 사용해
 * SELECT ... FOR UPDATE로 락을 걸고, 그 안에서 재고 차감·저장까지 한 트랜잭션으로 처리합니다.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@Testcontainers
@ActiveProfiles("test")
@Import(com.timedeal.api.infrastructure.config.TestSecurityConfig.class)
class PessimisticLockIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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

    @Test
    @DisplayName("동시 주문 시 비관적 락으로 재고가 정확히 차감됨")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 동시_주문_시_재고_정확히_차감() throws InterruptedException {
        // given: 이미 오픈된 상품, 재고 10개
        User user = User.builder()
                .email("concurrent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("동시주문테스트")
                .build();
        user = userRepository.save(user);
        Long userId = user.getId();

        Item item = Item.builder()
                .name("동시주문 상품")
                .price(new BigDecimal("10000"))
                .openTime(LocalDateTime.now().minusHours(1))
                .build();
        item = itemRepository.save(item);
        Long itemId = item.getId();

        Stock stock = Stock.builder()
                .item(item)
                .quantity(10)
                .build();
        stockRepository.save(stock);

        int threadCount = 10;
        int orderQtyEach = 1;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable orderTask = () -> {
            try {
                startLatch.await();
                OrderRequest req = new OrderRequest();
                req.setItemId(itemId);
                req.setQuantity(orderQtyEach);
                orderService.createOrder(userId, req);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(orderTask);
            }
            startLatch.countDown();
            endLatch.await();
        } finally {
            executor.shutdown();
        }

        // then: 10건 모두 성공하고, 재고는 0
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isZero();

        Stock result = stockRepository.findByItemId(itemId).orElseThrow();
        assertThat(result.getQuantity()).isZero();
    }
}
