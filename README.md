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
| **락 전략 비교 (B-3)** | 비관적 vs 낙관적 락, order-spike(재고 충분) | 비관적: RPS ≈ 162, p95 ≈ 935ms, 0% 실패. 낙관적: RPS ≈ 145, p95 ≈ 1170ms, 23% 실패 → 스파이크 시 비관적 락 유리 |
| **내부 로직 시간** | `OrderService.createOrder()` Micrometer Timer | 평균 **≈ 3~4ms**, 최대 **≈ 112ms** → 병목은 DB I/O·네트워크·필터 체인 등 외부 요소에 가까움 |

---

## 이 프로젝트에서 한 일

- **동시성 제어**: 비관적 락(`SELECT ... FOR UPDATE`)으로 재고 정합성 보장, 낙관적 락 경로 추가 후 성능·실패률 비교 측정 (B-3)
- **성능 테스트**: k6로 기본 READ·타임딜 스파이크·Soak·캐시(B-2)·락 전략(B-3) 시나리오 수행, PERF_RESULT·PERF_TEST_PLAN 문서화
- **운영·문서**: Hikari 풀 조정(B-1), Caffeine 캐시(B-2), 배포·모니터링 가이드, 비관적 vs 낙관적 락 비교 문서, 낙관적 락 실패 시 4xx 처리로 5xx 방지

---

## 기술적 결정·트레이드오프

| 주제 | 선택 | 이유·트레이드오프 |
|------|------|-------------------|
| **재고 동시성** | 비관적 락 기본, 낙관적 락 옵션 | 동일 상품에 주문이 몰리는 스파이크에서는 비관적 락이 처리량·성공률·에러율 모두 유리. 낙관적 락은 충돌 시 재시도·실패가 많아 부하 구간에선 불리 → `order.lock-strategy`로 전환 가능하도록 유지 |
| **상품 조회 성능** | Caffeine 캐시(목록·상세) | basic-read 부하에서 p95 약 70% 개선. 주문 몰리는 시나리오엔 영향 적음 |
| **낙관적 락 실패 응답** | 4xx(INSUFFICIENT_STOCK) | 재시도 후에도 실패하면 클라이언트는 "재고 부족"으로 처리하면 됨. 5xx로 나가면 원인 파악이 어려우므로 전역 핸들러에서 낙관적 락 예외 → 4xx로 매핑 |

---

## 관련 문서

- [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md) — 프로젝트 구조, 레이어, API, 테스트 가이드
- [docs/README.md](./docs/README.md) — 문서 목차 (가이드·성능·운영 문서 한눈에)
- [docs/deployment-monitoring.md](./docs/deployment-monitoring.md) — 배포·모니터링 가이드 (실행 방법, 헬스·메트릭·로그)
- [docs/perf/PERF_TEST_PLAN.md](./docs/perf/PERF_TEST_PLAN.md) — 성능 테스트 계획·SLO·시나리오
- [docs/perf/PERF_RESULT.md](./docs/perf/PERF_RESULT.md) — 성능 테스트 실측 결과 리포트
- [docs/lock-strategy-comparison.md](./docs/lock-strategy-comparison.md) — 비관적 vs 낙관적 락 비교 (한 페이지 요약)
- [PESSIMISTIC_LOCK_GUIDE.md](./PESSIMISTIC_LOCK_GUIDE.md) — 비관적 락 개념·구현·동작 원리·SQL
