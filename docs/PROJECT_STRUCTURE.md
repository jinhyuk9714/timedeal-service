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
9. [관련 문서](#관련-문서)
10. [최근 업데이트 내역](#최근-업데이트-내역)

---

## 프로젝트 개요

타임딜 서비스는 특정 시간에 오픈되는 상품에 대한 주문을 처리하는 Spring Boot 기반 REST API 서비스입니다.

### 주요 기능

- ✅ **사용자 관리**: 회원가입, 조회, 페이징
- ✅ **상품 관리**: 등록, 조회, 수정, 삭제, 페이징
- ✅ **타임딜 주문**: 오픈 시간 체크, 재고 관리, 주문 생성/조회/취소
- ✅ **동시성 제어**: 비관적 락(`SELECT ... FOR UPDATE`)으로 재고 정확성 보장
- ✅ **JWT 인증/인가**: 로그인, 로그아웃, 토큰 블랙리스트(Redis)
- ✅ **관리자 기능**: 역할 변경, 전체 주문/사용자/상품 관리(ADMIN 전용)
- ✅ **페이징**: 상품/주문/사용자 목록 API 페이징 지원
- ✅ **Swagger/OpenAPI**: API 문서 자동 생성 및 Swagger UI 제공
- ✅ **상품 검색/필터**: Querydsl 기반 상품명(부분 일치), 가격·오픈시간 범위 필터
- ✅ **Spring Boot Actuator**: 헬스 체크, 메트릭, 애플리케이션 정보 노출 (운영/모니터링)

---

## 아키텍처 구조

이 프로젝트는 **계층형 아키텍처(Layered Architecture)**를 따릅니다.

```
┌─────────────────────────────────────┐
│      Controller Layer (API)         │  ← HTTP 요청/응답, ApiPaths 사용
├─────────────────────────────────────┤
│      Service Layer (비즈니스 로직)   │  ← 주문/재고/인증/관리자 로직
├─────────────────────────────────────┤
│   Repository Layer (데이터 접근)    │  ← JPA, 비관적 락 지원
├─────────────────────────────────────┤
│      Domain Layer (도메인 모델)     │  ← 엔티티, UserRole 등
└─────────────────────────────────────┘
```

### 레이어 간 데이터 흐름

```
HTTP Request
    ↓
Controller (요청 검증, DTO 변환, @Valid)
    ↓
Service (비즈니스 로직, 트랜잭션, 비관적 락 등)
    ↓
Repository (데이터베이스 접근)
    ↓
Domain (엔티티 조작)
    ↓
Database / Redis
```

---

## 레이어별 설명

### 1. Controller Layer (`controller` 패키지)

**역할**: HTTP 요청을 받아 처리하고 응답을 반환. URL은 `ApiPaths` 상수 사용.

**주요 클래스**:

| 클래스 | 설명 |
|--------|------|
| `ItemController` | 상품 CRUD, 페이징 목록 |
| `UserController` | 회원가입, 사용자 조회 |
| `OrderController` | 주문 생성/조회/취소, 내 주문 목록(페이징), JWT 인증 필요 |
| `AuthController` | 로그인, 로그아웃 |
| `AdminController` | 전체 주문/사용자/상품 관리, 역할 변경, `@PreAuthorize("hasRole('ADMIN')")` |

**주요 어노테이션**:

- `@RestController`, `@RequestMapping(ApiPaths.XXX)`
- `@PageableDefault`, `Pageable` (페이징)
- `@ParameterObject` (Swagger + Pageable)
- `@AuthenticationPrincipal` (주문 API 등)

---

### 2. Service Layer (`service` 패키지)

**역할**: 비즈니스 로직 처리.

**주요 클래스**:

| 클래스 | 설명 |
|--------|------|
| `ItemService` | 상품 CRUD, 재고 연동, 페이징 목록 |
| `UserService` | 회원가입, 사용자 조회, 페이징 목록 |
| `OrderService` | 주문 생성(타임딜/재고 체크, **비관적 락**), 조회, 취소, 페이징 |
| `AuthService` | 로그인(JWT 발급), 로그아웃(토큰 블랙리스트) |
| `AdminService` | 상품 수정/삭제, 사용자 역할 변경, 전체 주문/사용자 페이징 |

**트랜잭션**: `@Transactional(readOnly = true)` 클래스 기본, 쓰기 메서드에 `@Transactional` 사용.

---

### 3. Repository Layer (`infrastructure/persistence` 패키지)

**역할**: 데이터베이스 접근.

**주요 인터페이스**:

- `ItemRepository`: 상품 (extends `ItemRepositoryCustom`), `ItemRepositoryImpl`: Querydsl 검색 구현
- `UserRepository`: 사용자 (`findByEmail`, `existsByEmail`)
- `OrderRepository`: 주문 (`findByUserId(Pageable)`, `findByItemId`)
- `StockRepository`: 재고, **비관적 락** `findByItemIdWithLock(@Lock(PESSIMISTIC_WRITE))`

---

### 4. Domain Layer (`domain` 패키지)

**주요 엔티티/Enum**:

- `User`, `UserRole`(USER, ADMIN)
- `Item`, `Stock`
- `Order`, `OrderStatus`(ORDERED, CANCELED)

**도메인 관계**:

```
User (1) ──< (N) Order (N) >── (1) Item
                                    │ (1:1)
                                    ↓
                                  Stock
```

---

### 5. DTO Layer (`dto` 패키지)

- **item**: `ItemRequest`, `ItemResponse`, `ItemSearchCondition` (검색 조건)
- **user**: `UserRequest`, `UserResponse`
- **order**: `OrderRequest`, `OrderResponse`
- **auth**: `LoginRequest`, `LoginResponse`
- **admin**: `ChangeRoleRequest`

---

### 6. Exception Layer (`exception` 패키지)

- `BusinessException`, `ErrorCode`, `ErrorResponse`, `GlobalExceptionHandler`
- `MethodArgumentNotValidException` / `BindException` → 400, `INVALID_INPUT_VALUE`

---

### 7. Common & Infrastructure

- **common**: `ApiPaths` — API 경로 상수 (`/api/items`, `/api/orders` 등)
- **infrastructure/config**: `QuerydslConfig`, `RedisConfig`, `SwaggerConfig`, `SpringDocQuerydslFixConfig`
- **infrastructure/config/health**: `DatabaseHealthIndicator`, `RedisHealthIndicator` (커스텀 헬스 체크)
- **infrastructure/security**: `JwtTokenProvider`, `JwtAuthenticationFilter`, `TokenBlacklistService`, `SecurityConfig`

---

## 주요 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot 4.0.2, Spring Data JPA, Hibernate 7.x, Querydsl 5.1.0, MySQL 8.0, Redis 7(JWT 블랙리스트) |
| **인증** | JWT(jjwt), BCrypt, Spring Security |
| **문서/UI** | Springdoc OpenAPI 3.0.1 (Swagger UI) |
| **Build/Test** | Gradle 8.14, JUnit 5, Mockito, Testcontainers 1.20.4 |
| **기타** | Lombok, P6Spy, Jakarta EE |

---

## 프로젝트 디렉토리 구조

```
timedeal-service/
├── src/main/java/com/timedeal/api/
│   ├── common/
│   │   └── ApiPaths.java              # API 경로 상수
│   ├── controller/
│   │   ├── ItemController.java
│   │   ├── UserController.java
│   │   ├── OrderController.java
│   │   ├── AuthController.java
│   │   └── AdminController.java
│   ├── service/
│   │   ├── ItemService.java
│   │   ├── UserService.java
│   │   ├── OrderService.java
│   │   ├── AuthService.java
│   │   └── AdminService.java
│   ├── domain/
│   │   ├── item/Item.java
│   │   ├── user/User.java, UserRole.java
│   │   ├── order/Order.java, OrderStatus.java
│   │   └── stock/Stock.java
│   ├── dto/
│   │   ├── item/ItemRequest.java, ItemResponse.java, ItemSearchCondition.java
│   │   ├── user/UserRequest.java, UserResponse.java
│   │   ├── order/OrderRequest.java, OrderResponse.java
│   │   ├── auth/LoginRequest.java, LoginResponse.java
│   │   └── admin/ChangeRoleRequest.java
│   ├── exception/
│   │   ├── BusinessException.java
│   │   ├── ErrorCode.java
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   ├── infrastructure/
│   │   ├── config/
│   │   │   ├── QuerydslConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── SwaggerConfig.java
│   │   │   ├── SpringDocQuerydslFixConfig.java
│   │   │   └── health/
│   │   │       ├── DatabaseHealthIndicator.java
│   │   │       └── RedisHealthIndicator.java
│   │   ├── persistence/
│   │   │   ├── item/ItemRepository.java, ItemRepositoryCustom.java, ItemRepositoryImpl.java
│   │   │   ├── user/UserRepository.java
│   │   │   ├── order/OrderRepository.java
│   │   │   └── stock/StockRepository.java
│   │   └── security/
│   │       ├── JwtTokenProvider.java
│   │       ├── JwtAuthenticationFilter.java
│   │       ├── TokenBlacklistService.java
│   │       └── SecurityConfig.java
│   └── TimeDealApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── spy.properties
├── src/test/java/com/timedeal/api/
│   ├── support/
│   │   ├── TestFixtures.java          # 테스트 데이터 픽스처
│   │   └── WebTestSupport.java        # ObjectMapper 등 공통 설정
│   ├── controller/
│   │   ├── ItemControllerTest.java
│   │   ├── UserControllerTest.java
│   │   ├── OrderControllerTest.java
│   │   ├── AuthControllerTest.java
│   │   └── AdminControllerTest.java
│   ├── service/
│   │   ├── ItemServiceTest.java
│   │   ├── UserServiceTest.java
│   │   ├── OrderServiceTest.java
│   │   ├── AuthServiceTest.java
│   │   └── AdminServiceTest.java
│   ├── exception/
│   │   └── GlobalExceptionHandlerTest.java
│   ├── infrastructure/config/
│   │   └── TestSecurityConfig.java
│   ├── integration/
│   │   ├── TimeDealIntegrationTest.java
│   │   └── PessimisticLockIntegrationTest.java   # 비관적 락 동시성 테스트
│   └── TimeDealApplicationTest.java
├── build.gradle
├── settings.gradle
├── compose.yaml
├── README.md
└── docs/
    ├── README.md                 # 문서 목차
    ├── PROJECT_STRUCTURE.md      # 이 문서
    ├── deployment-monitoring.md
    ├── lock-strategy-comparison.md
    ├── README-GRADLE-WRAPPER.md
    ├── guides/
    │   ├── JWT_GUIDE.md
    │   ├── PAGING_GUIDE.md
    │   ├── PESSIMISTIC_LOCK_GUIDE.md
    │   ├── POSTMAN_SETUP_GUIDE.md
    │   └── POSTMAN_ENDPOINTS_GUIDE.md
    └── perf/
        ├── PERF_TEST_PLAN.md
        └── PERF_RESULT.md
```

---

## 핵심 개념 설명

### 1. 의존성 주입 (DI)

- 생성자 주입 + `@RequiredArgsConstructor`, `final` 필드 사용.

### 2. 트랜잭션

- 쓰기 메서드에 `@Transactional`, 읽기 전용은 `@Transactional(readOnly = true)`.

### 3. 비관적 락 (Pessimistic Lock)

- `StockRepository.findByItemIdWithLock()` → `SELECT ... FOR UPDATE`
- 주문 생성 시 재고 조회·차감 구간에서만 사용. 자세한 내용은 [guides/PESSIMISTIC_LOCK_GUIDE.md](guides/PESSIMISTIC_LOCK_GUIDE.md) 참고.

### 4. JPA 영속성 컨텍스트

- 1차 캐시, 변경 감지, 지연 로딩 등 JPA 기본 동작 사용.

---

## API 엔드포인트

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| **인증** | | | |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/logout` | 로그아웃 | Bearer |
| **사용자** | | | |
| POST | `/api/users` | 회원가입 | X |
| GET | `/api/users/{id}` | 사용자 조회 | X |
| **상품** | | | |
| POST | `/api/items` | 상품 등록 | X |
| GET | `/api/items/{id}` | 상품 조회 | X |
| GET | `/api/items?page=&size=&sort=&name=&minPrice=&maxPrice=&openAfter=&openBefore=` | 상품 목록(페이징·검색/필터) | X |
| **주문** | | | |
| POST | `/api/orders` | 주문 생성 | Bearer |
| GET | `/api/orders/{id}` | 주문 조회 | Bearer |
| GET | `/api/orders/my-orders?page=&size=&sort=` | 내 주문 목록(페이징) | Bearer |
| PATCH | `/api/orders/{id}/cancel` | 주문 취소 | Bearer |
| **관리자** | | | |
| GET | `/api/admin/orders?page=&size=&sort=` | 전체 주문 목록(페이징) | Bearer(ADMIN) |
| GET | `/api/admin/users?page=&size=&sort=` | 전체 사용자 목록(페이징) | Bearer(ADMIN) |
| PUT | `/api/admin/items/{id}` | 상품 수정 | Bearer(ADMIN) |
| DELETE | `/api/admin/items/{id}` | 상품 삭제 | Bearer(ADMIN) |
| PATCH | `/api/admin/users/{id}/role` | 사용자 역할 변경 | Bearer(ADMIN) |
| **Actuator** | | | |
| GET | `/actuator/health` | 헬스 체크 (DB, Redis 상태 포함) | X |
| GET | `/actuator/info` | 애플리케이션 정보 | X |
| GET | `/actuator/metrics` | 메트릭 목록 | Bearer |
| GET | `/actuator/metrics/{name}` | 특정 메트릭 조회 | Bearer |
| GET | `/actuator/prometheus` | Prometheus 형식 메트릭 | Bearer |

**문서**: Swagger UI → `http://localhost:8080/swagger-ui.html`  
**모니터링**: Actuator → `http://localhost:8080/actuator`

---

## 테스트 가이드

### 테스트 종류

| 구분 | 위치 | 설명 |
|------|------|------|
| **Controller** | `controller/*Test.java` | `@WebMvcTest`, MockMvc, `ApiPaths`/`TestFixtures`/`WebTestSupport` 사용 |
| **Service** | `service/*Test.java` | Mockito, `TestFixtures`, 비관적 락 메서드 호출 검증 포함 |
| **Exception** | `exception/GlobalExceptionHandlerTest.java` | BusinessException, BindException, Exception 처리 검증 |
| **통합** | `integration/TimeDealIntegrationTest.java` | Testcontainers MySQL, 전체 플로우 |
| **비관적 락** | `integration/PessimisticLockIntegrationTest.java` | 동시 주문 시 재고 정확성 검증 |

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 클래스/메서드
./gradlew test --tests "com.timedeal.api.service.OrderServiceTest"
./gradlew test --tests "*.PessimisticLockIntegrationTest.동시_주문_시_재고_정확히_차감"
```

### 테스트 지원

- **TestFixtures**: `user()`, `item()`, `itemOpened()`, `stock()`, `order()`, `itemRequest()`, `orderRequest()` 등
- **WebTestSupport**: `objectMapper()` (JavaTimeModule 등록)
- **ApiPaths**: URL 하드코딩 대신 상수 사용

### Spring Boot 4.0 테스트

- `@MockitoBean` 사용 (Spring Boot 4.0)
- `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- `testcontext.bean.override.mockito.MockitoBean` 사용

---

## 관련 문서

| 문서 | 설명 |
|------|------|
| [guides/JWT_GUIDE.md](guides/JWT_GUIDE.md) | JWT 인증 흐름, 로그인/로그아웃 사용법 |
| [guides/PAGING_GUIDE.md](guides/PAGING_GUIDE.md) | 페이징 개념, Pageable/Page 사용법, API 예시 |
| [guides/PESSIMISTIC_LOCK_GUIDE.md](guides/PESSIMISTIC_LOCK_GUIDE.md) | 비관적 락 동작 원리, SQL, 주의사항 |
| [guides/POSTMAN_ENDPOINTS_GUIDE.md](guides/POSTMAN_ENDPOINTS_GUIDE.md) | 엔드포인트별 요청/응답 예시 |
| [guides/POSTMAN_SETUP_GUIDE.md](guides/POSTMAN_SETUP_GUIDE.md) | Postman JWT 자동 저장 등 설정 |

---

## 최근 업데이트 내역

### 2026-01-28

- ✅ **Spring Boot Actuator**: 헬스 체크(`/actuator/health`), 메트릭(`/actuator/metrics`), 애플리케이션 정보(`/actuator/info`) 노출
- ✅ **커스텀 HealthIndicator**: `DatabaseHealthIndicator`, `RedisHealthIndicator` 추가 (DB/Redis 연결 상태 상세 확인)
- ✅ **보안 설정**: Actuator 엔드포인트 보안 설정 (`/actuator/health`, `/actuator/info`는 공개, 나머지는 인증 필요)
- ✅ **상품 검색/필터**: `ItemSearchCondition`(name, minPrice, maxPrice, openAfter, openBefore) + Querydsl `ItemRepositoryImpl.findByCondition()` 적용
- ✅ **GET /api/items**: 선택 쿼리 파라미터로 검색·필터 지원, 조건 없으면 기존과 동일하게 전체 목록 반환
- ✅ **테스트**: `ItemServiceTest.getItems_WithCondition_CallsFindByCondition`, `getItems_NoCondition_CallsFindAll`, `ItemControllerTest.getItems_WithSearchCondition_Success` 추가

### 2026-01-27

- ✅ **공통 상수**: `ApiPaths` 도입, Controller 경로 통일
- ✅ **테스트**: `TestFixtures`, `WebTestSupport` 추가, Controller/Service/Exception 테스트 보강
- ✅ **비관적 락 테스트**: `OrderServiceTest.createOrder_비관적_락_메서드_사용_검증`, `PessimisticLockIntegrationTest.동시_주문_시_재고_정확히_차감` 추가
- ✅ **GlobalExceptionHandlerTest**: BindException 기반 검증 예외 테스트 추가

### 2026-01-26

- ✅ Spring Boot 4.0.2, JWT 인증/인가, 로그아웃(Redis 블랙리스트)
- ✅ 관리자 기능(역할 변경, 전체 주문/사용자/상품 관리)
- ✅ 페이징(상품/주문/사용자 목록), Swagger/OpenAPI 문서화
- ✅ 상품 삭제 시 주문 존재 여부 검증(`ITEM_CANNOT_BE_DELETED`)

---

**작성일**: 2026-01-26  
**최종 업데이트**: 2026-01-28  
**버전**: 1.3.0
