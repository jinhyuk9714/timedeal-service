# 타임딜 서비스 (Timedeal Service)

특정 시간에 오픈되는 상품에 대한 주문을 처리하는 **Spring Boot 기반 REST API** 서비스입니다.  
비관적 락으로 재고 정합성을 보장하고, JWT·Redis·Querydsl을 활용한 계층형 아키텍처로 구성되어 있습니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot 4.0.2, Java 21, Spring Data JPA, Hibernate 7.x, Querydsl 5.1.0, MySQL 8.0, Redis 7 |
| **인증** | JWT(jjwt), BCrypt, Spring Security |
| **문서/모니터링** | Springdoc OpenAPI 3.0 (Swagger UI), Spring Boot Actuator |
| **Build/Test** | Gradle 8.14, JUnit 5, Mockito, Testcontainers |

---

## 주요 기능

- **사용자/상품/주문 관리**: 회원가입·조회, 상품 CRUD·페이징, 타임딜 주문·조회·취소
- **동시성 제어**: 비관적 락(`SELECT ... FOR UPDATE`)으로 재고 정확성 보장
- **인증/인가**: JWT 로그인·로그아웃, 토큰 블랙리스트(Redis), ADMIN 전용 관리 API
- **상품 검색**: Querydsl 기반 상품명·가격·오픈시간 범위 필터
- **API 문서**: Swagger UI (`/swagger-ui.html`), 헬스·메트릭(`/actuator/**`)

상세 구조와 API 엔드포인트는 **[PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)**를 참고하세요.

---

## 실행 방법

1. **MySQL·Redis 기동** (Docker Compose)
   ```bash
   docker compose up -d
   ```
2. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```
   - Swagger UI: http://localhost:8080/swagger-ui.html  
   - Actuator 헬스: http://localhost:8080/actuator/health

---

## 성능 테스트 요약

k6와 `perf` 프로파일로 부하·스파이크·Soak 시나리오를 수행한 결과 요약입니다.  
상세 계획·수치는 **[docs/perf/PERF_TEST_PLAN.md](./docs/perf/PERF_TEST_PLAN.md)**, **[docs/perf/PERF_RESULT.md](./docs/perf/PERF_RESULT.md)**를 참고하세요.

| 시나리오 | 조건 | 결과 요약 |
|----------|------|-----------|
| **기본 READ** | 상품 목록/상세 | 약 **40 RPS**, 에러율 0%, **p95 &lt; 50ms** |
| **타임딜 스파이크 – 재고 부족** | 0→200 VU, 주문 집중 | 약 **165 RPS**, 전부 400 INSUFFICIENT_STOCK, **p95 ≈ 950ms**, 데이터 정합성 유지 |
| **타임딜 스파이크 – 재고 충분** | 인덱스 적용, 주문 성공 | 약 **160 RPS**, **주문 6505건 201 성공**, 에러율 0%, **p95 ≈ 960ms** |
| **Soak (장시간 부하)** | 10 VU · 10분, 로그인+목록/상세+주문 혼합 | **에러율 0%**, **p95 ≈ 118ms**, 약 **29 RPS** 안정 동작 |
| **캐시 도입 (B-2, Caffeine)** | 상품 목록·상세 캐시 적용 후 basic-read 20 VU 60s | **p95 38.8ms → 11.6ms**(약 70% 개선), RPS ≈ 39.5, 에러율 0% |
| **내부 로직 시간** | `OrderService.createOrder()` Micrometer Timer | 평균 **≈ 3~4ms**, 최대 **≈ 112ms** → 병목은 DB I/O·네트워크·필터 체인 등 외부 요소에 가까움 |

---

## 관련 문서

- [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) — 프로젝트 구조, 레이어, API, 테스트 가이드
- [docs/perf/PERF_TEST_PLAN.md](./docs/perf/PERF_TEST_PLAN.md) — 성능 테스트 계획·SLO·시나리오
- [docs/perf/PERF_RESULT.md](./docs/perf/PERF_RESULT.md) — 성능 테스트 실측 결과 리포트
- [PESSIMISTIC_LOCK_GUIDE.md](./PESSIMISTIC_LOCK_GUIDE.md) — 비관적 락 사용 가이드
