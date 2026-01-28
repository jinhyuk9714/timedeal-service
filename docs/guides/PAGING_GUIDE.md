# 페이징(Paging) 가이드

## 📚 목차
1. [페이징이란?](#페이징이란)
2. [왜 페이징이 필요한가?](#왜-페이징이-필요한가)
3. [Spring Data JPA 페이징 구조](#spring-data-jpa-페이징-구조)
4. [현재 프로젝트 구현 방식](#현재-프로젝트-구현-방식)
5. [사용 방법](#사용-방법)
6. [응답 형식](#응답-형식)
7. [코드 예시](#코드-예시)

---

## 페이징이란?

**페이징(Paging)**은 대량의 데이터를 작은 단위(페이지)로 나누어 전송하는 기법입니다.

### 예시
- ❌ **페이징 없음**: 10,000개의 상품을 한 번에 조회 → 느림, 메모리 부족
- ✅ **페이징 있음**: 10,000개의 상품을 20개씩 나누어 조회 → 빠름, 효율적

```
전체 데이터: [1, 2, 3, ..., 10,000]
페이징 적용:
  - 1페이지: [1, 2, 3, ..., 20]
  - 2페이지: [21, 22, 23, ..., 40]
  - 3페이지: [41, 42, 43, ..., 60]
  ...
```

---

## 왜 페이징이 필요한가?

### 1. **성능 향상**
- **메모리 효율**: 필요한 데이터만 메모리에 로드
- **네트워크 효율**: 작은 데이터만 전송하여 응답 시간 단축
- **DB 부하 감소**: `LIMIT`과 `OFFSET`을 사용하여 필요한 데이터만 조회

### 2. **사용자 경험 개선**
- 사용자가 한 번에 볼 수 있는 적절한 양의 데이터 제공
- 스크롤이 너무 길어지는 것을 방지

### 3. **서버 안정성**
- 대량 데이터 조회로 인한 서버 다운 방지
- 동시 접속자 증가 시에도 안정적인 서비스 제공

---

## Spring Data JPA 페이징 구조

### 핵심 클래스

#### 1. `Pageable` 인터페이스
페이징 정보를 담는 인터페이스입니다.

```java
// 주요 메서드
int getPageNumber();      // 현재 페이지 번호 (0부터 시작)
int getPageSize();        // 페이지 크기 (한 페이지에 보여줄 항목 수)
Sort getSort();           // 정렬 정보
```

#### 2. `Page<T>` 인터페이스
페이징된 데이터와 메타 정보를 담는 인터페이스입니다.

```java
// 주요 메서드
List<T> getContent();           // 실제 데이터 리스트
long getTotalElements();        // 전체 항목 수
int getTotalPages();            // 전체 페이지 수
int getSize();                  // 페이지 크기
int getNumber();                // 현재 페이지 번호
boolean isFirst();              // 첫 페이지 여부
boolean isLast();               // 마지막 페이지 여부
int getNumberOfElements();     // 현재 페이지의 항목 수
```

#### 3. `PageRequest` 클래스
`Pageable`의 구현체입니다.

```java
// 생성 방법
PageRequest.of(0, 20);                    // 0번째 페이지, 20개씩
PageRequest.of(0, 20, Sort.by("id").descending());  // 정렬 포함
```

---

## 현재 프로젝트 구현 방식

### 1. Repository 레이어

Spring Data JPA의 `JpaRepository`는 자동으로 페이징을 지원합니다.

```java
// OrderRepository.java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 페이징 지원 메서드
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    // 페이징 없이 전체 조회 (기존 호환성 유지)
    List<Order> findByUserId(Long userId);
}
```

**핵심**: `Pageable`을 파라미터로 받으면 자동으로 `Page<T>`를 반환합니다.

### 2. Service 레이어

```java
// ItemService.java
public Page<ItemResponse> getAllItems(Pageable pageable) {
    // 1. Repository에서 페이징된 데이터 조회
    Page<Item> itemPage = itemRepository.findAll(pageable);
    
    // 2. Page의 content를 DTO로 변환
    return itemPage.map(item -> {
        Stock stock = stockRepository.findByItemId(item.getId())
                .orElse(null);
        return new ItemResponse(item, stock);
    });
}
```

**설명**:
- `itemRepository.findAll(pageable)`: 페이징 쿼리 실행
- `itemPage.map(...)`: 각 `Item`을 `ItemResponse`로 변환
- 반환 타입이 `Page<ItemResponse>`이므로 페이징 정보도 함께 반환됨

### 3. Controller 레이어

```java
// ItemController.java
@GetMapping
public ResponseEntity<Page<ItemResponse>> getAllItems(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
        Pageable pageable) {
    Page<ItemResponse> responses = itemService.getAllItems(pageable);
    return ResponseEntity.ok(responses);
}
```

**핵심 어노테이션**:
- `@PageableDefault`: 페이징 파라미터가 없을 때 기본값 설정
  - `size = 20`: 기본 페이지 크기
  - `sort = "id"`: 기본 정렬 필드
  - `direction = Sort.Direction.DESC`: 내림차순

### 4. 설정 파일 (application.yml)

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20      # 기본 페이지 크기
        max-page-size: 100          # 최대 페이지 크기
        page-parameter: page        # 페이지 번호 파라미터명
        size-parameter: size        # 페이지 크기 파라미터명
        sort: id,desc               # 기본 정렬
```

---

## 사용 방법

### API 호출 예시

#### 1. 기본 호출 (기본값 사용)
```
GET /api/items
```
→ 기본값: `page=0`, `size=20`, `sort=id,desc`

#### 2. 페이지 지정
```
GET /api/items?page=0&size=10
```
→ 0번째 페이지, 10개씩

#### 3. 정렬 포함
```
GET /api/items?page=0&size=20&sort=createdAt,desc
```
→ 생성일 기준 내림차순

#### 4. 여러 필드로 정렬
```
GET /api/items?page=0&size=20&sort=price,asc&sort=id,desc
```
→ 가격 오름차순, ID 내림차순

#### 5. 실제 사용 예시
```bash
# 첫 페이지 (20개)
curl http://localhost:8080/api/items

# 두 번째 페이지 (10개씩)
curl http://localhost:8080/api/items?page=1&size=10

# 가격 낮은 순으로 정렬
curl http://localhost:8080/api/items?sort=price,asc

# 복합 정렬
curl http://localhost:8080/api/items?page=0&size=20&sort=price,asc&sort=id,desc
```

---

## 응답 형식

### JSON 응답 구조

```json
{
  "content": [                    // 실제 데이터 배열
    {
      "id": 1,
      "name": "타임딜 상품 1",
      "price": 10000.00,
      "stockQuantity": 100,
      "openTime": "2026-01-26T10:00:00",
      "createdAt": "2026-01-26T09:00:00",
      "updatedAt": "2026-01-26T09:00:00"
    },
    {
      "id": 2,
      "name": "타임딜 상품 2",
      "price": 20000.00,
      "stockQuantity": 50,
      "openTime": "2026-01-26T11:00:00",
      "createdAt": "2026-01-26T09:30:00",
      "updatedAt": "2026-01-26T09:30:00"
    }
    // ... 최대 20개 (size에 따라 다름)
  ],
  "pageable": {                   // 페이징 요청 정보
    "pageNumber": 0,              // 현재 페이지 번호
    "pageSize": 20,               // 페이지 크기
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 100,           // 전체 항목 수
  "totalPages": 5,                // 전체 페이지 수
  "last": false,                  // 마지막 페이지 여부
  "first": true,                  // 첫 페이지 여부
  "size": 20,                     // 페이지 크기
  "number": 0,                    // 현재 페이지 번호
  "numberOfElements": 20,         // 현재 페이지의 항목 수
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "empty": false                  // 빈 페이지 여부
}
```

### 필드 설명

| 필드 | 설명 | 예시 |
|------|------|------|
| `content` | 실제 데이터 배열 | `[{...}, {...}]` |
| `totalElements` | 전체 항목 수 | `100` |
| `totalPages` | 전체 페이지 수 | `5` (100개 ÷ 20개 = 5페이지) |
| `size` | 페이지 크기 | `20` |
| `number` | 현재 페이지 번호 (0부터 시작) | `0` (첫 페이지) |
| `first` | 첫 페이지 여부 | `true` |
| `last` | 마지막 페이지 여부 | `false` |
| `numberOfElements` | 현재 페이지의 항목 수 | `20` |

---

## 코드 예시

### 1. Repository에서 페이징 사용

```java
// OrderRepository.java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 페이징 지원
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    // 조건 + 페이징
    Page<Order> findByStatusAndUserId(OrderStatus status, Long userId, Pageable pageable);
    
    // 정렬 포함 페이징
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

### 2. Service에서 페이징 처리

```java
// OrderService.java
public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
    // Repository에서 페이징된 데이터 조회
    Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
    
    // Page의 content를 DTO로 변환
    return orderPage.map(OrderResponse::new);
}
```

### 3. Controller에서 페이징 파라미터 받기

```java
// OrderController.java
@GetMapping("/my-orders")
public ResponseEntity<Page<OrderResponse>> getMyOrders(
        @AuthenticationPrincipal Long authenticatedUserId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    
    Page<OrderResponse> responses = orderService.getUserOrders(authenticatedUserId, pageable);
    return ResponseEntity.ok(responses);
}
```

### 4. 수동으로 PageRequest 생성

```java
// Service나 테스트 코드에서 사용
Pageable pageable = PageRequest.of(0, 20);  // 0번째 페이지, 20개씩

// 정렬 포함
Pageable pageable = PageRequest.of(
    0, 
    20, 
    Sort.by("createdAt").descending()
);

// 여러 필드로 정렬
Pageable pageable = PageRequest.of(
    0,
    20,
    Sort.by("price").ascending()
        .and(Sort.by("id").descending())
);
```

---

## 실제 SQL 쿼리

Spring Data JPA가 페이징을 사용하면 다음과 같은 SQL이 생성됩니다:

```sql
-- 페이징 쿼리 (MySQL)
SELECT * FROM item 
ORDER BY id DESC 
LIMIT 20 OFFSET 0;  -- 첫 페이지 (0~19번째)

-- 두 번째 페이지
SELECT * FROM item 
ORDER BY id DESC 
LIMIT 20 OFFSET 20;  -- 두 번째 페이지 (20~39번째)

-- 전체 개수 조회 (totalElements 계산용)
SELECT COUNT(*) FROM item;
```

**설명**:
- `LIMIT`: 가져올 항목 수 (size)
- `OFFSET`: 건너뛸 항목 수 (page × size)

---

## 주의사항

### 1. 페이지 번호는 0부터 시작
```java
// ❌ 잘못된 사용
PageRequest.of(1, 20);  // 1번째 페이지가 아니라 2번째 페이지!

// ✅ 올바른 사용
PageRequest.of(0, 20);  // 첫 페이지
PageRequest.of(1, 20);  // 두 번째 페이지
```

### 2. 대량 데이터 정렬 시 성능 주의
```java
// ❌ 인덱스가 없는 필드로 정렬하면 느림
sort=name,asc  // name에 인덱스가 없으면 느림

// ✅ 인덱스가 있는 필드로 정렬 (일반적으로 id, createdAt)
sort=id,desc
sort=createdAt,desc
```

### 3. max-page-size 설정 확인
```yaml
spring:
  data:
    web:
      pageable:
        max-page-size: 100  # 최대 100개까지만 허용
```
→ 클라이언트가 `size=1000`을 요청해도 최대 100개만 반환됩니다.

---

## 테스트 코드 예시

```java
// AdminServiceTest.java
@Test
@DisplayName("전체 주문 목록 조회 성공 (페이징)")
void getAllOrders_Success() {
    // given: 페이징 정보 생성
    Pageable pageable = PageRequest.of(0, 20);
    
    // Mock 데이터
    List<Order> orders = Arrays.asList(order1, order2);
    Page<Order> orderPage = new PageImpl<>(orders, pageable, orders.size());
    
    when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);
    
    // when: 페이징된 데이터 조회
    Page<OrderResponse> responses = adminService.getAllOrders(pageable);
    
    // then: 검증
    assertThat(responses.getContent()).hasSize(2);
    assertThat(responses.getTotalElements()).isEqualTo(2);
    assertThat(responses.getNumber()).isEqualTo(0);  // 첫 페이지
}
```

---

## 요약

1. **페이징은 대량 데이터를 작은 단위로 나누어 처리하는 기법**
2. **Spring Data JPA는 `Pageable`과 `Page<T>`로 페이징을 자동 지원**
3. **Repository에 `Pageable`을 추가하면 자동으로 페이징 쿼리 생성**
4. **Controller에서 `@PageableDefault`로 기본값 설정 가능**
5. **응답에는 데이터(`content`)와 페이징 정보(`totalElements`, `totalPages` 등)가 포함됨**

---

## 참고 자료

- [Spring Data JPA 공식 문서 - Pagination](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation)
- [Spring Data Commons - Pageable](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/domain/Pageable.html)
