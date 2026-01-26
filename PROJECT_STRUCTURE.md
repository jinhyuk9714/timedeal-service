# 타임딜 서비스 프로젝트 구조 가이드

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [아키텍처 구조](#아키텍처-구조)
3. [레이어별 설명](#레이어별-설명)
4. [주요 기술 스택](#주요-기술-스택)
5. [프로젝트 디렉토리 구조](#프로젝트-디렉토리-구조)
6. [핵심 개념 설명](#핵심-개념-설명)
7. [API 엔드포인트](#api-엔드포인트)
8. [테스트 가이드](#테스트-가이드)

---

## 프로젝트 개요

타임딜 서비스는 특정 시간에 오픈되는 상품에 대한 주문을 처리하는 Spring Boot 기반 REST API 서비스입니다.

### 주요 기능
- ✅ 사용자 관리 (회원가입, 조회)
- ✅ 상품 관리 (등록, 조회)
- ✅ 타임딜 주문 (오픈 시간 체크, 재고 관리)
- ✅ 주문 취소 (재고 복구)
- ✅ 동시성 제어 (비관적 락)

---

## 아키텍처 구조

이 프로젝트는 **계층형 아키텍처(Layered Architecture)**를 따릅니다.

```
┌─────────────────────────────────────┐
│      Controller Layer (API)        │  ← HTTP 요청/응답 처리
├─────────────────────────────────────┤
│      Service Layer (비즈니스 로직)   │  ← 핵심 비즈니스 로직
├─────────────────────────────────────┤
│   Repository Layer (데이터 접근)    │  ← 데이터베이스 접근
├─────────────────────────────────────┤
│      Domain Layer (도메인 모델)     │  ← 엔티티 및 비즈니스 규칙
└─────────────────────────────────────┘
```

### 레이어 간 데이터 흐름

```
HTTP Request
    ↓
Controller (요청 검증, DTO 변환)
    ↓
Service (비즈니스 로직 실행)
    ↓
Repository (데이터베이스 접근)
    ↓
Domain (엔티티 조작)
    ↓
Database
```

---

## 레이어별 설명

### 1. Controller Layer (`controller` 패키지)

**역할**: HTTP 요청을 받아서 처리하고 응답을 반환

**주요 클래스**:
- `ItemController`: 상품 관련 API
- `UserController`: 사용자 관련 API
- `OrderController`: 주문 관련 API

**Spring 어노테이션**:
- `@RestController`: REST API 컨트롤러
- `@RequestMapping`: URL 매핑
- `@GetMapping`, `@PostMapping`, `@PatchMapping`: HTTP 메서드 매핑
- `@PathVariable`: URL 경로 변수
- `@RequestBody`: 요청 본문을 객체로 변환
- `@Valid`: DTO 유효성 검증

**예시**:
```java
@RestController
@RequestMapping("/api/items")
public class ItemController {
    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemRequest request) {
        // ...
    }
}
```

---

### 2. Service Layer (`service` 패키지)

**역할**: 비즈니스 로직을 처리하는 핵심 레이어

**주요 클래스**:
- `ItemService`: 상품 비즈니스 로직
- `UserService`: 사용자 비즈니스 로직
- `OrderService`: 주문 비즈니스 로직 (타임딜 시간 체크, 재고 관리)

**Spring 어노테이션**:
- `@Service`: 서비스 레이어 빈 등록
- `@Transactional`: 트랜잭션 관리
  - `readOnly = true`: 읽기 전용 트랜잭션 (성능 최적화)
  - 메서드 레벨에서 `@Transactional` 사용 시 쓰기 트랜잭션

**트랜잭션 관리**:
```java
@Service
@Transactional(readOnly = true)  // 기본값: 읽기 전용
public class OrderService {
    
    @Transactional  // 이 메서드는 쓰기 트랜잭션
    public OrderResponse createOrder(...) {
        // 여러 DB 작업이 하나의 트랜잭션으로 처리됨
    }
}
```

---

### 3. Repository Layer (`infrastructure/persistence` 패키지)

**역할**: 데이터베이스 접근을 담당

**주요 인터페이스**:
- `ItemRepository`: 상품 데이터 접근
- `UserRepository`: 사용자 데이터 접근
- `OrderRepository`: 주문 데이터 접근
- `StockRepository`: 재고 데이터 접근 (비관적 락 지원)

**Spring Data JPA**:
- `JpaRepository<Entity, ID>`를 상속받아 기본 CRUD 메서드 제공
- 메서드 이름으로 쿼리 자동 생성
- `@Query`: 커스텀 쿼리 작성
- `@Lock`: 비관적 락 설정

**예시**:
```java
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)  // 비관적 락
    @Query("SELECT s FROM Stock s WHERE s.item.id = :itemId")
    Optional<Stock> findByItemIdWithLock(@Param("itemId") Long itemId);
}
```

---

### 4. Domain Layer (`domain` 패키지)

**역할**: 비즈니스 도메인 모델과 규칙을 표현

**주요 엔티티**:
- `User`: 사용자
- `Item`: 상품
- `Stock`: 재고
- `Order`: 주문
- `OrderStatus`: 주문 상태 enum

**JPA 어노테이션**:
- `@Entity`: JPA 엔티티
- `@Table`: 테이블 이름 지정
- `@Id`, `@GeneratedValue`: 기본키 설정
- `@ManyToOne`, `@OneToOne`: 연관관계 매핑
- `@PrePersist`, `@PreUpdate`: 생명주기 콜백

**도메인 모델 관계**:
```
User (1) ──< (N) Order (N) >── (1) Item
                                    │
                                    │ (1:1)
                                    ↓
                                  Stock
```

---

### 5. DTO Layer (`dto` 패키지)

**역할**: 계층 간 데이터 전송 객체

**구조**:
- `Request`: 클라이언트 → 서버 (요청 데이터)
- `Response`: 서버 → 클라이언트 (응답 데이터)

**주요 DTO**:
- `ItemRequest`, `ItemResponse`
- `UserRequest`, `UserResponse`
- `OrderRequest`, `OrderResponse`

**유효성 검증**:
- `@NotNull`, `@NotBlank`, `@Email`, `@Positive` 등
- Controller에서 `@Valid`로 검증 활성화

---

### 6. Exception Layer (`exception` 패키지)

**역할**: 예외 처리 및 에러 응답 관리

**주요 클래스**:
- `BusinessException`: 비즈니스 예외
- `ErrorCode`: 에러 코드 enum
- `ErrorResponse`: 에러 응답 DTO
- `GlobalExceptionHandler`: 전역 예외 처리

**예외 처리 흐름**:
```
비즈니스 로직에서 예외 발생
    ↓
BusinessException 던짐
    ↓
GlobalExceptionHandler가 캐치
    ↓
ErrorResponse로 변환하여 반환
```

---

## 주요 기술 스택

### Backend
- **Spring Boot 4.0.2**: 애플리케이션 프레임워크
- **Spring Data JPA**: 데이터베이스 접근
- **Hibernate 7.x**: ORM 프레임워크
- **Querydsl 5.1.0**: 타입 안전한 쿼리 작성
- **MySQL 8.0**: 관계형 데이터베이스
- **Redis 7**: 캐시/세션 저장소 (향후 사용)

### Build & Test
- **Gradle 8.14**: 빌드 도구
- **JUnit 5**: 단위 테스트
- **Testcontainers 1.20.4**: 통합 테스트 (Docker 컨테이너)
- **Mockito**: Mock 객체 생성 및 검증
- **Jackson**: JSON 직렬화/역직렬화 (테스트용)

### 기타
- **Lombok**: 보일러플레이트 코드 제거
- **P6Spy**: SQL 쿼리 로깅
- **Jakarta EE**: Java EE의 후속 버전

---

## 프로젝트 디렉토리 구조

```
timedeal-service/
├── src/
│   ├── main/
│   │   ├── java/com/timedeal/api/
│   │   │   ├── controller/          # REST API 컨트롤러
│   │   │   │   ├── ItemController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── OrderController.java
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   │   ├── ItemService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── OrderService.java
│   │   │   ├── domain/              # 도메인 모델
│   │   │   │   ├── item/
│   │   │   │   │   └── Item.java
│   │   │   │   ├── user/
│   │   │   │   │   └── User.java
│   │   │   │   ├── order/
│   │   │   │   │   ├── Order.java
│   │   │   │   │   └── OrderStatus.java
│   │   │   │   └── stock/
│   │   │   │       └── Stock.java
│   │   │   ├── dto/                 # 데이터 전송 객체
│   │   │   │   ├── item/
│   │   │   │   │   ├── ItemRequest.java
│   │   │   │   │   └── ItemResponse.java
│   │   │   │   ├── user/
│   │   │   │   │   ├── UserRequest.java
│   │   │   │   │   └── UserResponse.java
│   │   │   │   └── order/
│   │   │   │       ├── OrderRequest.java
│   │   │   │       └── OrderResponse.java
│   │   │   ├── exception/           # 예외 처리
│   │   │   │   ├── BusinessException.java
│   │   │   │   ├── ErrorCode.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── infrastructure/      # 인프라스트럭처
│   │   │   │   ├── config/
│   │   │   │   │   └── QuerydslConfig.java
│   │   │   │   └── persistence/
│   │   │   │       ├── item/
│   │   │   │       │   └── ItemRepository.java
│   │   │   │       ├── user/
│   │   │   │       │   └── UserRepository.java
│   │   │   │       ├── order/
│   │   │   │       │   └── OrderRepository.java
│   │   │   │       └── stock/
│   │   │   │           └── StockRepository.java
│   │   │   └── TimeDealApplication.java
│   │   └── resources/
│   │       ├── application.yml      # 설정 파일
│   │       └── spy.properties      # P6Spy 설정
│   └── test/
│       └── java/com/timedeal/api/
│           ├── controller/          # Controller 테스트
│           │   └── ItemControllerTest.java
│           ├── service/             # Service 테스트
│           │   └── OrderServiceTest.java
│           ├── integration/         # 통합 테스트
│           │   └── TimeDealIntegrationTest.java
│           └── TimeDealApplicationTest.java
├── build.gradle                     # Gradle 빌드 설정
├── settings.gradle                  # Gradle 프로젝트 설정
├── compose.yaml                     # Docker Compose 설정
└── README.md                        # 프로젝트 설명
```

---

## 핵심 개념 설명

### 1. 의존성 주입 (Dependency Injection, DI)

Spring이 객체 간 의존성을 자동으로 주입해주는 기능입니다.

**생성자 주입 방식** (권장):
```java
@Service
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
public class OrderService {
    private final OrderRepository orderRepository;  // final로 선언
    // Spring이 자동으로 OrderRepository 구현체를 주입
}
```

**장점**:
- 불변성 보장 (final 키워드)
- 테스트 용이 (Mock 객체 주입 쉬움)
- 순환 참조 방지

---

### 2. 트랜잭션 (Transaction)

여러 데이터베이스 작업을 하나의 작업 단위로 묶는 개념입니다.

**특징**:
- **원자성(Atomicity)**: 모두 성공하거나 모두 실패
- **일관성(Consistency)**: 데이터 무결성 유지
- **격리성(Isolation)**: 동시 실행 시 격리
- **지속성(Durability)**: 커밋 후 영구 저장

**사용 예시**:
```java
@Transactional
public OrderResponse createOrder(...) {
    stock.decrease(quantity);      // 1. 재고 차감
    orderRepository.save(order);   // 2. 주문 저장
    // 둘 중 하나라도 실패하면 전체 롤백
}
```

---

### 3. 비관적 락 (Pessimistic Lock)

동시성 문제를 해결하기 위한 방법입니다.

**비관적 락**:
- 데이터를 조회할 때 락을 걸어서 다른 트랜잭션이 수정하지 못하게 함
- `SELECT ... FOR UPDATE` 쿼리 실행
- 동시 접근이 많을 때 사용

**사용 예시**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Stock s WHERE s.item.id = :itemId")
Optional<Stock> findByItemIdWithLock(@Param("itemId") Long itemId);
```

---

### 4. JPA 영속성 컨텍스트 (Persistence Context)

엔티티를 관리하는 영역입니다.

**특징**:
- 1차 캐시: 같은 엔티티를 조회하면 캐시에서 반환
- 변경 감지: 엔티티 변경 시 자동으로 UPDATE 쿼리 실행
- 지연 로딩: 연관 엔티티를 필요할 때만 조회

**예시**:
```java
Item item = itemRepository.findById(1L);  // DB 조회
item.setName("변경된 이름");                // 엔티티 수정
// save() 호출 없이도 UPDATE 쿼리 자동 실행 (변경 감지)
```

---

## API 엔드포인트

### 상품 (Items)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/items` | 상품 등록 |
| GET | `/api/items/{id}` | 상품 조회 |
| GET | `/api/items` | 전체 상품 목록 |

### 사용자 (Users)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/users` | 사용자 등록 |
| GET | `/api/users/{id}` | 사용자 조회 |

### 주문 (Orders)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/orders/users/{userId}` | 주문 생성 |
| GET | `/api/orders/{id}` | 주문 조회 |
| GET | `/api/orders/users/{userId}` | 사용자별 주문 목록 |
| PATCH | `/api/orders/{id}/cancel` | 주문 취소 |

---

## 테스트 가이드

### 테스트 종류

1. **Controller 테스트** (`controller/ItemControllerTest.java`)
   - `@WebMvcTest`: 웹 레이어만 테스트
   - `MockMvc`: HTTP 요청/응답 시뮬레이션
   - `@MockitoBean`: Service를 Mock으로 대체 (Spring Boot 4.0)
   - `ObjectMapper`: JSON 변환 (직접 생성 필요)

2. **Service 단위 테스트** (`service/OrderServiceTest.java`)
   - Service 레이어만 테스트
   - Mockito로 Repository Mock 생성
   - 비즈니스 로직 검증

3. **통합 테스트** (`integration/TimeDealIntegrationTest.java`)
   - `@SpringBootTest`: 전체 컨텍스트 로드
   - Testcontainers로 실제 MySQL 사용
   - 전체 플로우 테스트 (사용자 생성 → 상품 등록 → 주문)

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests TimeDealIntegrationTest

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

### 테스트 작성 시 주의사항

#### 1. Spring Boot 4.0 변경사항

**@MockBean → @MockitoBean**
```java
// Spring Boot 3.x (deprecated)
@MockBean
private ItemService itemService;

// Spring Boot 4.0 (권장)
@MockitoBean
private ItemService itemService;
```

**@WebMvcTest 패키지 변경**
```java
// Spring Boot 3.x
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// Spring Boot 4.0
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

#### 2. ObjectMapper 직접 생성

`@WebMvcTest`와 `@SpringBootTest`에서 `ObjectMapper`가 자동 주입되지 않을 수 있으므로 직접 생성:

```java
@BeforeEach
void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 지원
}
```

#### 3. Testcontainers 설정

```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
}
```

---

## 학습 포인트

### Spring Boot 핵심 개념

1. **@SpringBootApplication**: 애플리케이션 진입점
2. **@ComponentScan**: 빈 자동 스캔
3. **@Autowired**: 의존성 자동 주입
4. **@Transactional**: 트랜잭션 관리
5. **@RestController**: REST API 컨트롤러

### JPA 핵심 개념

1. **Entity**: 데이터베이스 테이블과 매핑
2. **Repository**: 데이터 접근 추상화
3. **연관관계**: @ManyToOne, @OneToOne
4. **영속성 컨텍스트**: 엔티티 관리 영역
5. **지연 로딩**: 필요할 때만 조회

### 아키텍처 패턴

1. **계층형 아키텍처**: Controller → Service → Repository
2. **DTO 패턴**: 계층 간 데이터 전송
3. **예외 처리**: 전역 예외 핸들러
4. **의존성 주입**: 생성자 주입 방식

---

## 추가 학습 자료

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Spring Data JPA 문서](https://spring.io/projects/spring-data-jpa)
- [JPA 공식 문서](https://jakarta.ee/specifications/persistence/)

---

---

## 최근 업데이트 내역

### 2026-01-26
- ✅ Spring Boot 4.0.2로 업그레이드
- ✅ 테스트 코드 작성 완료 (Controller, Service, Integration)
- ✅ `@MockitoBean` 사용 (Spring Boot 4.0 대응)
- ✅ `ObjectMapper` 직접 생성 방식 적용
- ✅ 모든 테스트 통과 확인

---

**작성일**: 2026-01-26  
**최종 업데이트**: 2026-01-26  
**버전**: 1.1.0
