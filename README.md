# Timedeal Service

Timedeal Service는 특정 시간에 열리는 상품 주문을 처리하는 Spring Boot REST API입니다. 재고 차감이 몰리는 상황을 기준으로 락 전략, 캐시, rate limiting, circuit breaker, 모니터링, k6 성능 테스트를 함께 다룹니다.

## 문제 의식

타임딜 주문은 짧은 시간에 같은 상품으로 요청이 집중됩니다. 이 저장소는 주문 정합성을 지키기 위한 비관적 락, 낙관적 락, Redis 분산 락을 구현하고, 조회 캐시와 resilience 설정이 응답 시간과 실패율에 어떤 영향을 주는지 측정 문서로 남겼습니다.

## 주요 기능

- 회원가입, 로그인, 로그아웃과 JWT 토큰 블랙리스트
- 사용자, 상품, 주문, 관리자 API
- 상품 검색과 페이징, Querydsl 조건 조회
- 타임딜 주문 생성, 조회, 취소
- 재고 동시성 제어 전략 전환: `order.lock-strategy`
- Caffeine 기반 상품 목록/상세 캐시
- Resilience4j rate limiting과 circuit breaker
- Actuator, Prometheus, Grafana 대시보드
- k6 부하 테스트와 성능 결과 문서

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.0.2, Spring Web, Spring Security |
| Persistence | Spring Data JPA, Hibernate 7, Querydsl, MySQL |
| Cache / Lock | Redis, Caffeine |
| Resilience | Resilience4j |
| Observability | Actuator, Micrometer Prometheus, P6Spy, Prometheus, Grafana |
| Test / Perf | JUnit 5, Mockito, Testcontainers, k6 |
| Build | Gradle |

## 구조

```text
src/main/java/com/timedeal/api/
├── controller/        # Auth, User, Item, Order, Admin API
├── domain/            # User, Item, Stock, Order
├── dto/               # 요청/응답 DTO
├── exception/         # ErrorCode, GlobalExceptionHandler
├── infrastructure/    # security, persistence, lock, config, logging
└── service/           # 도메인 서비스

docs/                  # 구조, 성능, 락 전략, 운영 가이드
perf/k6/               # k6 시나리오
monitoring/            # Prometheus, Grafana 설정
```

## 실행 방법

MySQL, Redis, Prometheus, Grafana는 Compose로 실행합니다.

```bash
docker compose up -d
```

애플리케이션은 Gradle로 실행합니다.

```bash
./gradlew bootRun
```

테스트는 다음 명령으로 실행합니다.

```bash
./gradlew test
```

기본 접속 주소는 다음과 같습니다.

| 서비스 | 주소 |
| --- | --- |
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Actuator Health | `http://localhost:8080/actuator/health` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

## 성능 테스트

`perf/k6/`에는 읽기 부하, 주문 스파이크, 장시간 혼합 부하, 분산 주문 시나리오가 포함되어 있습니다.

```bash
k6 run perf/k6/basic-read.js
k6 run perf/k6/order-spike.js
k6 run perf/k6/soak-mixed.js
k6 run perf/k6/order-spike-distributed.js
```

상세한 조건과 결과는 `docs/perf/PERF_TEST_PLAN.md`, `docs/perf/PERF_RESULT.md`, `docs/lock-strategy-comparison.md`에 정리되어 있습니다.

## 관련 문서

- `docs/PROJECT_STRUCTURE.md`: 레이어, API, 테스트 가이드
- `docs/deployment-monitoring.md`: 배포와 모니터링
- `docs/INTERVIEW-GUIDE.md`: 발표/면접용 요약
- `docs/guides/JWT_GUIDE.md`: JWT 사용 흐름
- `docs/guides/PESSIMISTIC_LOCK_GUIDE.md`: 비관적 락 설명
- `docs/guides/PAGING_GUIDE.md`: 페이징 정리
