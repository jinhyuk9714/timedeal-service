# 타임딜 서비스 (Timedeal Service)

특정 시간에 오픈되는 상품에 대한 주문을 처리하는 **Spring Boot 기반 REST API** 서비스입니다.  
**동시성 제어·성능 최적화·레질리언스 패턴**을 실전 수준으로 구현하고, k6 부하 테스트로 검증한 프로젝트입니다.

## 핵심 가치

- **동시성 제어**: 비관적·낙관적·분산 락(Redis) 세 가지 전략을 구현하고, 동일 부하 조건에서 성능·성공률·에러율을 비교 측정
- **성능 최적화**: 캐시(Caffeine), 커넥션 풀 튜닝, 인덱스 최적화를 적용하고 "적용 전/후" 수치로 검증
- **레질리언스**: Rate limiting·Circuit breaker(Resilience4j)로 과부하·장애 전파 방지, 실측 데이터로 효과 검증
- **실전 수준의 문서화**: 성능 테스트 계획·결과, 락 전략 비교, 배포·모니터링 가이드까지 체계적으로 정리

---

## 기술 스택

| 구분              | 기술                                                                                           |
| ----------------- | ---------------------------------------------------------------------------------------------- |
| **Backend**       | Spring Boot 4.0.2, Java 21, Spring Data JPA, Hibernate 7.x, Querydsl 5.1.0, MySQL 8.0, Redis 7 |
| **인증**          | JWT(jjwt), BCrypt, Spring Security                                                             |
| **문서/모니터링** | Springdoc OpenAPI 3.0 (Swagger UI), Spring Boot Actuator                                       |
| **Build/Test**    | Gradle 8.14, JUnit 5, Mockito, Testcontainers                                                  |

---

## 주요 기능

- **사용자/상품/주문 관리**: 회원가입·조회, 상품 CRUD·페이징, 타임딜 주문·조회·취소
- **동시성 제어**: 비관적·낙관적·분산 락(Redis) 세 가지 전략을 프로퍼티로 전환 가능 (`order.lock-strategy`)
- **인증/인가**: JWT 로그인·로그아웃, 토큰 블랙리스트(Redis), ADMIN 전용 관리 API
- **상품 검색**: Querydsl 기반 상품명·가격·오픈시간 범위 필터
- **레질리언스**: Rate limiting(초당 요청 제한), Circuit breaker(장애 전파 방지)
- **API 문서**: Swagger UI (`/swagger-ui.html`), 헬스·메트릭(`/actuator/**`)

상세 구조와 API 엔드포인트는 **[docs/PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md)**를 참고하세요.

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
   - **Prometheus + Grafana**: `docker compose up -d` 시 Prometheus(9090)·Grafana(3000)도 기동. 대시보드·접속 방법은 [배포·모니터링 가이드](docs/deployment-monitoring.md) 6절 참고.

---

## 성능 테스트 요약

k6와 `perf` 프로파일로 부하·스파이크·Soak 시나리오를 수행한 결과 요약입니다.  
상세 계획·수치는 **[docs/perf/PERF_TEST_PLAN.md](./docs/perf/PERF_TEST_PLAN.md)**, **[docs/perf/PERF_RESULT.md](./docs/perf/PERF_RESULT.md)**를 참고하세요.

| 시나리오                                     | 조건                                                        | 결과 요약                                                                                                                                                             |
| -------------------------------------------- | ----------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **기본 READ**                                | 상품 목록/상세                                              | 약 **40 RPS**, 에러율 0%, **p95 &lt; 50ms**                                                                                                                           |
| **타임딜 스파이크 – 재고 부족**              | 0→200 VU, 주문 집중                                         | 약 **165 RPS**, 전부 400 INSUFFICIENT_STOCK, **p95 ≈ 950ms**, 데이터 정합성 유지                                                                                      |
| **타임딜 스파이크 – 재고 충분**              | 인덱스 적용, 주문 성공                                      | 약 **160 RPS**, **주문 6505건 201 성공**, 에러율 0%, **p95 ≈ 960ms**                                                                                                  |
| **Soak (장시간 부하)**                       | 10 VU · 10분, 로그인+목록/상세+주문 혼합                    | **에러율 0%**, **p95 ≈ 118ms**, 약 **29 RPS** 안정 동작                                                                                                               |
| **캐시 도입 (실험 3)**                       | 상품 목록·상세 캐시 적용 후 basic-read 20 VU 60s            | **p95 38.8ms → 11.6ms**(약 70% 개선), RPS ≈ 39.5, 에러율 0%                                                                                                           |
| **락 전략 비교 (실험 4)**                    | 비관적 vs 낙관적 vs 분산 락(Redis), order-spike(재고 충분)  | 비관적: RPS ≈ 162, p95 ≈ 935ms, 0% 실패. 낙관적: RPS ≈ 145, p95 ≈ 1170ms, 23% 실패. 분산 락: RPS ≈ 132, p95 ≈ 1770ms, 42.61% 실패 → 스파이크 시 비관적 락이 가장 유리 |
| **Rate limiting / Circuit breaker (실험 5)** | Resilience4j 적용, order-spike(재고 충분)                   | 초당 100건 제한으로 8건 429 차단, 성공률 99.87% 유지, RPS 약간 감소하지만 시스템 부하 완화 효과                                                                       |
| **상품 분산 주문 (시나리오 C)**              | order-spike-distributed, 0→200 VU, 전체 상품 재고·오픈 충분 | **RPS ≈ 247**, **p95 ≈ 863ms**, 주문 6685건 201 성공, 에러율 0%. 동일 아이템 몰림보다 p95 약간 개선                                                                   |
| **장애 시나리오 (7절)**                      | DB/Redis 일시 중단·복구                                     | DB 중단 시 100% 실패, 복구 후 정상 수렴. Redis 중단 시 order-spike는 영향 없음(로그아웃 미호출)                                                                       |
| **내부 로직 시간**                           | `OrderService.createOrder()` Micrometer Timer               | 평균 **≈ 3~4ms**, 최대 **≈ 112ms** → 병목은 DB I/O·네트워크·필터 체인 등 외부 요소에 가까움                                                                           |

---

## 이 프로젝트에서 한 일

### 동시성 제어

- **비관적·낙관적·분산 락(Redis) 세 가지 전략** 구현 및 프로퍼티로 전환 가능 (`order.lock-strategy`)
- 동일 부하 조건(order-spike, 0→200 VU)에서 **성능·성공률·에러율 비교 측정**
- 결과: 비관적 락이 스파이크 구간에서 가장 유리, 분산 락은 멀티 인스턴스 환경에서 유리

### 성능 최적화

- **캐시(Caffeine)**: 상품 목록·상세 캐시로 basic-read p95 **70% 개선** (38.8ms → 11.6ms)
- **커넥션 풀 튜닝**: Hikari 풀 크기 조정 실험 (현재 부하에서는 30/10으로 충분)
- **인덱스 최적화**: 주문·재고 조회 인덱스 적용

### 레질리언스 패턴

- **Rate limiting**: Resilience4j로 주문 API 초당 100건 제한, 초과 요청 429 차단
- **Circuit breaker**: DB 장애 시 빠른 실패(503)로 리소스 보호
- 실측 데이터로 효과 검증 (성공률 99.87% 유지, 시스템 부하 완화)

### 성능 테스트·문서화

- k6로 **기본 READ·타임딜 스파이크·Soak·캐시·락 전략·Rate limiting/Circuit breaker·장애 시나리오** 시나리오 수행
- **PERF_RESULT·PERF_TEST_PLAN** 체계적 문서화 (공통 조건 → Baseline → 실험별 비교)
- **락 전략 비교 문서**, 배포·모니터링 가이드, JWT·페이징 등 가이드 문서화

---

## 기술적 결정·트레이드오프

| 주제                    | 선택                                  | 이유·트레이드오프                                                                                                                                                                                                                                                                                               |
| ----------------------- | ------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **재고 동시성**         | 비관적 락 기본, 낙관적·분산 락 옵션   | 동일 상품에 주문이 몰리는 스파이크에서는 비관적 락이 처리량·성공률·에러율 모두 가장 유리. 낙관적 락은 version 충돌·재시도 후 실패가 많고, 분산 락(Redis)은 락 획득 실패로 성공률이 가장 낮음. 단일 서버에서는 비관적 락이 적합, 멀티 인스턴스에서는 분산 락 고려 → `order.lock-strategy`로 전환 가능하도록 유지 |
| **상품 조회 성능**      | Caffeine 캐시(목록·상세)              | basic-read 부하에서 p95 약 70% 개선 (38.8ms → 11.6ms). 주문 몰리는 시나리오엔 영향 적음                                                                                                                                                                                                                         |
| **낙관적 락 실패 응답** | 4xx(INSUFFICIENT_STOCK)               | 재시도 후에도 실패하면 클라이언트는 "재고 부족"으로 처리하면 됨. 5xx로 나가면 원인 파악이 어려우므로 전역 핸들러에서 낙관적 락 예외 → 4xx로 매핑                                                                                                                                                                |
| **Rate limiting**       | Resilience4j, 초당 100건 제한         | 초과 요청을 429로 차단하여 시스템 부하 완화. 성공률 99.87% 유지하며 안정성 향상                                                                                                                                                                                                                                 |
| **Circuit breaker**     | Resilience4j, 실패율 50% 초과 시 차단 | DB 장애 시 빠른 실패(503)로 리소스 보호. DB 정상 시에는 동작하지 않으나 장애 대응에 유리                                                                                                                                                                                                                        |

---

## 관련 문서

- [docs/README.md](./docs/README.md) — 문서 목차 (가이드·성능·운영 문서 한눈에)
- [docs/PROJECT_STRUCTURE.md](./docs/PROJECT_STRUCTURE.md) — 프로젝트 구조, 레이어, API, 테스트 가이드
- [docs/deployment-monitoring.md](./docs/deployment-monitoring.md) — 배포·모니터링 가이드 (실행 방법, 헬스·메트릭·로그)
- [docs/INTERVIEW-GUIDE.md](./docs/INTERVIEW-GUIDE.md) — 인터뷰/발표용 요약 및 예상 질문·답변 정리
- [docs/perf/PERF_TEST_PLAN.md](./docs/perf/PERF_TEST_PLAN.md) — 성능 테스트 계획·SLO·시나리오
- [docs/perf/PERF_RESULT.md](./docs/perf/PERF_RESULT.md) — 성능 테스트 실측 결과 리포트
- [docs/lock-strategy-comparison.md](./docs/lock-strategy-comparison.md) — 비관적 vs 낙관적 vs 분산 락 비교 (한 페이지 요약)
- [docs/guides/PESSIMISTIC_LOCK_GUIDE.md](./docs/guides/PESSIMISTIC_LOCK_GUIDE.md) — 비관적 락 개념·구현·동작 원리·SQL
- [docs/guides/JWT_GUIDE.md](./docs/guides/JWT_GUIDE.md) — JWT 인증 흐름, 로그인/로그아웃 사용법
- [docs/guides/PAGING_GUIDE.md](./docs/guides/PAGING_GUIDE.md) — 페이징 개념, Pageable/Page 사용법
- [docs/guides/POSTMAN_SETUP_GUIDE.md](./docs/guides/POSTMAN_SETUP_GUIDE.md) · [docs/guides/POSTMAN_ENDPOINTS_GUIDE.md](./docs/guides/POSTMAN_ENDPOINTS_GUIDE.md) — Postman 설정·엔드포인트 예시
