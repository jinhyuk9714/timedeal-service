# 비관적 락(Pessimistic Lock) 가이드

## 📚 목차
1. [비관적 락이란?](#비관적-락이란)
2. [왜 비관적 락이 필요한가?](#왜-비관적-락이-필요한가)
3. [현재 프로젝트 구현 방식](#현재-프로젝트-구현-방식)
4. [동작 원리](#동작-원리)
5. [실제 SQL 쿼리](#실제-sql-쿼리)
6. [비관적 락 vs 낙관적 락](#비관적-락-vs-낙관적-락)

---

## 비관적 락이란?

**비관적 락(Pessimistic Lock)**은 데이터를 조회할 때 **미리 락을 걸어서** 다른 트랜잭션이 해당 데이터를 수정하지 못하도록 막는 방식입니다.

### 특징
- **"비관적"**: 데이터 충돌이 발생할 것이라고 가정하고 미리 락을 걸음
- **SELECT ... FOR UPDATE**: 데이터베이스 레벨에서 락을 걸어 동시 접근 차단
- **동시성 문제 해결**: 여러 트랜잭션이 동시에 같은 데이터를 수정하는 것을 방지

---

## 왜 비관적 락이 필요한가?

### 문제 상황: 동시 주문 시 재고 오차

**시나리오**: 재고가 10개인 상품에 대해 2명의 사용자가 동시에 5개씩 주문

#### ❌ 비관적 락 없이 (Race Condition 발생)

```
시간 | 사용자 A                    | 사용자 B                    | 재고
-----|----------------------------|----------------------------|------
T1   | 재고 조회: 10개             |                            | 10
T2   |                            | 재고 조회: 10개             | 10
T3   | 재고 차감: 10 - 5 = 5       |                            | 5
T4   |                            | 재고 차감: 10 - 5 = 5       | 5 ❌
T5   | 주문 저장                   |                            | 5
T6   |                            | 주문 저장                   | 5

결과: 재고가 5개가 되어야 하는데, 실제로는 10개에서 5개만 차감됨!
      → 재고 오차 발생 (Race Condition)
```

#### ✅ 비관적 락 사용 (정확한 재고 관리)

```
시간 | 사용자 A                    | 사용자 B                    | 재고
-----|----------------------------|----------------------------|------
T1   | 재고 조회 (락 걸림)          |                            | 10 (락)
T2   |                            | 재고 조회 시도 → 대기...     | 10 (락)
T3   | 재고 차감: 10 - 5 = 5       |                            | 5 (락)
T4   | 주문 저장                   |                            | 5
T5   | 트랜잭션 커밋 (락 해제)      |                            | 5
T6   |                            | 재고 조회 (락 걸림)          | 5 (락)
T7   |                            | 재고 차감: 5 - 5 = 0        | 0 (락)
T8   |                            | 주문 저장                   | 0
T9   |                            | 트랜잭션 커밋 (락 해제)      | 0

결과: 재고가 정확하게 0개가 됨! ✅
```

---

## 현재 프로젝트 구현 방식

### 1. Repository 레이어: `StockRepository`

```java
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    // 비관적 락을 사용하는 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)  // ← 비관적 락 설정
    @Query("SELECT s FROM Stock s WHERE s.item.id = :itemId")
    Optional<Stock> findByItemIdWithLock(@Param("itemId") Long itemId);
    
    // 일반 조회 메서드 (락 없음)
    Optional<Stock> findByItemId(Long itemId);
}
```

**핵심 어노테이션**:
- `@Lock(LockModeType.PESSIMISTIC_WRITE)`: 비관적 쓰기 락 설정
  - `PESSIMISTIC_WRITE`: 다른 트랜잭션이 읽기/쓰기 모두 차단
  - `PESSIMISTIC_READ`: 다른 트랜잭션이 쓰기만 차단 (읽기는 가능)
- `@Query`: JPQL 쿼리 작성

### 2. Service 레이어: `OrderService`

```java
@Transactional
public OrderResponse createOrder(Long userId, OrderRequest request) {
    // ... 사용자, 상품 조회 ...
    
    // 4. 재고 확인 및 차감 (비관적 락 사용)
    Stock stock = stockRepository.findByItemIdWithLock(request.getItemId())
            .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
    
    // 5. 재고 부족 체크
    if (stock.getQuantity() < request.getQuantity()) {
        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
    }
    
    // 6. 재고 차감
    stock.decrease(request.getQuantity());
    stockRepository.save(stock);
    
    // 7. 주문 생성
    Order order = Order.builder()
            .user(user)
            .item(item)
            .status(OrderStatus.ORDERED)
            .quantity(request.getQuantity())
            .build();
    
    Order savedOrder = orderRepository.save(order);
    return new OrderResponse(savedOrder);
}
```

**핵심 포인트**:
1. `findByItemIdWithLock()` 호출 시 락이 걸림
2. 락이 걸린 동안 다른 트랜잭션은 대기
3. 트랜잭션이 커밋되면 락이 해제됨

---

## 동작 원리

### 1. 트랜잭션 시작
```java
@Transactional  // ← 트랜잭션 시작
public OrderResponse createOrder(...) {
    // ...
}
```

### 2. 비관적 락 적용
```java
Stock stock = stockRepository.findByItemIdWithLock(itemId);
// → SELECT * FROM stocks WHERE item_id = ? FOR UPDATE
// → 락이 걸림! 다른 트랜잭션은 대기...
```

### 3. 재고 차감
```java
stock.decrease(request.getQuantity());
stockRepository.save(stock);
// → UPDATE stocks SET quantity = ? WHERE id = ?
```

### 4. 주문 저장
```java
orderRepository.save(order);
// → INSERT INTO orders ...
```

### 5. 트랜잭션 커밋 (락 해제)
```java
// @Transactional 메서드 종료 시 자동 커밋
// → 락이 해제됨! 대기 중인 트랜잭션이 진행 가능
```

---

## 실제 SQL 쿼리

### 비관적 락이 적용된 쿼리

```sql
-- findByItemIdWithLock() 호출 시
SELECT * FROM stocks 
WHERE item_id = 1 
FOR UPDATE;  -- ← 락을 걸고 조회

-- 이 쿼리가 실행되는 동안:
-- 1. 다른 트랜잭션의 SELECT는 가능 (읽기 가능)
-- 2. 다른 트랜잭션의 UPDATE/DELETE는 대기 (쓰기 차단)
-- 3. 현재 트랜잭션이 커밋되면 락 해제
```

### 전체 흐름 (실제 SQL)

```sql
-- 트랜잭션 A 시작
BEGIN;

-- 1. 재고 조회 (락 걸림)
SELECT * FROM stocks WHERE item_id = 1 FOR UPDATE;
-- 결과: id=1, quantity=10

-- 2. 재고 차감
UPDATE stocks SET quantity = 5 WHERE id = 1;

-- 3. 주문 저장
INSERT INTO orders (user_id, item_id, quantity, status) 
VALUES (1, 1, 5, 'ORDERED');

-- 트랜잭션 A 커밋 (락 해제)
COMMIT;

-- ============================================

-- 트랜잭션 B 시작 (A가 커밋한 후)
BEGIN;

-- 1. 재고 조회 (락 걸림)
SELECT * FROM stocks WHERE item_id = 1 FOR UPDATE;
-- 결과: id=1, quantity=5 (A가 차감한 후)

-- 2. 재고 차감
UPDATE stocks SET quantity = 0 WHERE id = 1;

-- 3. 주문 저장
INSERT INTO orders (user_id, item_id, quantity, status) 
VALUES (2, 1, 5, 'ORDERED');

-- 트랜잭션 B 커밋 (락 해제)
COMMIT;
```

---

## 비관적 락 vs 낙관적 락

> **한 페이지 비교 정리**: 락 거는 시점·동작·적합한 상황·이 프로젝트 적용까지 요약한 문서는 [docs/lock-strategy-comparison.md](docs/lock-strategy-comparison.md)를 참고하면 된다.

### 비교표

| 항목 | 비관적 락 (Pessimistic Lock) | 낙관적 락 (Optimistic Lock) |
|------|------------------------------|------------------------------|
| **가정** | 충돌이 발생할 것 | 충돌이 거의 없을 것 |
| **방식** | 미리 락을 걸어 차단 | 버전 번호로 충돌 감지 |
| **성능** | 동시 접근 시 대기 발생 | 대기 없음 (재시도 필요) |
| **사용 시기** | 동시 수정이 빈번한 경우 | 동시 수정이 드문 경우 |
| **구현** | `SELECT ... FOR UPDATE` | `@Version` 필드 사용 |
| **예시** | 재고 차감, 계좌 이체 | 게시글 수정, 프로필 변경 |

### 비관적 락 사용 예시 (현재 프로젝트)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Stock s WHERE s.item.id = :itemId")
Optional<Stock> findByItemIdWithLock(@Param("itemId") Long itemId);
```

### 낙관적 락 사용 예시 (참고)

```java
@Entity
public class Stock {
    @Version  // ← 버전 필드
    private Long version;
    
    // ...
}

// Service에서
Stock stock = stockRepository.findById(id);
stock.decrease(quantity);
try {
    stockRepository.save(stock);  // version이 변경되면 예외 발생
} catch (OptimisticLockingFailureException e) {
    // 충돌 발생! 재시도 필요
}
```

---

## LockModeType 종류

### 1. `PESSIMISTIC_WRITE` (현재 사용)
- **용도**: 쓰기 작업 시 사용
- **동작**: 다른 트랜잭션의 읽기/쓰기 모두 차단
- **SQL**: `SELECT ... FOR UPDATE`
- **사용 예시**: 재고 차감, 계좌 이체

### 2. `PESSIMISTIC_READ`
- **용도**: 읽기 작업 시 사용
- **동작**: 다른 트랜잭션의 쓰기만 차단 (읽기는 가능)
- **SQL**: `SELECT ... FOR SHARE` (MySQL) 또는 `SELECT ... LOCK IN SHARE MODE`
- **사용 예시**: 읽기 후 수정할 때

### 3. `PESSIMISTIC_FORCE_INCREMENT`
- **용도**: 강제 버전 증가
- **동작**: 락을 걸고 버전도 증가시킴
- **사용 예시**: 복잡한 비즈니스 로직

---

## 주의사항

### 1. 데드락(Deadlock) 위험
```java
// ❌ 위험: 서로 다른 순서로 락을 걸면 데드락 발생 가능
// 트랜잭션 A: stock1 → stock2
// 트랜잭션 B: stock2 → stock1
// → 데드락 발생!

// ✅ 안전: 항상 같은 순서로 락을 걸기
// 모든 트랜잭션: stock1 → stock2 (ID 순서)
```

### 2. 락 타임아웃 설정
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
@Query("SELECT s FROM Stock s WHERE s.item.id = :itemId")
Optional<Stock> findByItemIdWithLock(@Param("itemId") Long itemId);
```
→ 5초 동안 락을 얻지 못하면 예외 발생

### 3. 트랜잭션 범위 최소화
```java
// ❌ 나쁜 예: 락을 오래 유지
@Transactional
public void processOrder() {
    Stock stock = findByItemIdWithLock(id);  // 락 걸림
    // ... 복잡한 비즈니스 로직 (시간 소요) ...
    stock.decrease(quantity);  // 락 해제
}

// ✅ 좋은 예: 락을 최소한으로 유지
@Transactional
public void processOrder() {
    // ... 복잡한 비즈니스 로직 ...
    Stock stock = findByItemIdWithLock(id);  // 락 걸림
    stock.decrease(quantity);  // 빠르게 락 해제
}
```

---

## 테스트 방법

### 동시성 테스트 예시

```java
@Test
@DisplayName("동시 주문 시 재고 정확성 테스트")
void concurrentOrderTest() throws InterruptedException {
    // given: 재고 10개
    Stock stock = Stock.builder()
            .item(item)
            .quantity(10)
            .build();
    stockRepository.save(stock);
    
    // when: 2명이 동시에 5개씩 주문
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(2);
    
    executor.submit(() -> {
        try {
            orderService.createOrder(userId1, request1);  // 5개 주문
        } finally {
            latch.countDown();
        }
    });
    
    executor.submit(() -> {
        try {
            orderService.createOrder(userId2, request2);  // 5개 주문
        } finally {
            latch.countDown();
        }
    });
    
    latch.await();
    
    // then: 재고가 정확하게 0개
    Stock result = stockRepository.findByItemId(item.getId()).orElseThrow();
    assertThat(result.getQuantity()).isEqualTo(0);
}
```

---

## 요약

1. **비관적 락은 데이터 충돌을 미리 방지하는 방식**
2. **`@Lock(LockModeType.PESSIMISTIC_WRITE)`로 설정**
3. **`SELECT ... FOR UPDATE` 쿼리로 락을 걸어 동시 접근 차단**
4. **재고 차감 같은 동시성 문제가 중요한 곳에서 사용**
5. **트랜잭션이 커밋되면 자동으로 락이 해제됨**

---

## 참고 자료

- [JPA Lock Modes](https://docs.oracle.com/javaee/7/api/javax/persistence/LockModeType.html)
- [Spring Data JPA - Locking](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#locking)
- [MySQL FOR UPDATE](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html)
