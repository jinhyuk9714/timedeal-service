# 배포·모니터링 가이드

로컬/성능 테스트용으로 앱을 띄우는 방법과, 헬스·메트릭·로그를 보는 방법을 한 페이지로 정리합니다.

---

## 1. 사전 요구 사항

- **Java 21**, **Gradle 8.x** (또는 `./gradlew` 사용)
- **Docker**, **Docker Compose** (MySQL·Redis용)

---

## 2. 인프라 기동 (MySQL, Redis)

```bash
docker compose up -d
```

- **MySQL**: `localhost:3306`, DB `timedeal`, 사용자 `timedeal` / `timedeal`
- **Redis**: `localhost:6379`
- 컨테이너 헬스체크 통과 후 앱 기동 권장 (`docker compose ps`로 상태 확인)

---

## 3. 애플리케이션 실행

### 기본 실행 (로컬 개발)

```bash
./gradlew bootRun
```

- **프로파일**: `local` (기본)
- **포트**: 8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### 프로파일별 실행

| 목적 | 프로파일 | 실행 예시 |
|------|----------|-----------|
| 로컬 개발 | `local` | `./gradlew bootRun` (또는 `--args='--spring.profiles.active=local'`) |
| 성능 테스트 | `perf` | `./gradlew bootRun --args='--spring.profiles.active=perf'` |
| 성능 테스트 (낙관적 락) | `perf` + `perf-optimistic` | `./gradlew bootRun --args='--spring.profiles.active=perf,perf-optimistic'` |

- **perf** 사용 시: `ddl-auto: validate`이므로 스키마는 미리 준비해 두어야 함. 필요 시 DB에 `stocks.version` 등 DDL 적용 후 기동.
- **perf-optimistic**: `order.lock-strategy: optimistic`만 덮어쓰므로, `perf`와 함께 사용.

---

## 4. 헬스 체크

| 엔드포인트 | 용도 |
|------------|------|
| `GET /actuator/health` | 서버 up/down 및 DB·Redis 등 의존성 상태 |

- **형식**: JSON. `status`: `UP` / `DOWN`, `components`에 db·redis 등 상세 포함.
- **설정**: `management.endpoint.health.show-details`에 따라 상세 노출 여부 달라짐 (기본 `when-authorized`).

배포 환경에서는 로드밸런서·컨테이너 오케스트레이터에서 이 URL을 주기적으로 호출해 정상 여부(살아 있는지) 판단에 사용하면 된다.

---

## 5. 메트릭·Prometheus

| 엔드포인트 | 용도 |
|------------|------|
| `GET /actuator/metrics` | 사용 가능한 메트릭 이름 목록 |
| `GET /actuator/metrics/{name}` | 특정 메트릭 값 (예: `jvm.memory.used`, `timedeal.order.create`) |
| `GET /actuator/prometheus` | Prometheus 스크래핑용 텍스트 형식 |

- **노출 메트릭**: `health`, `info`, `metrics`, `prometheus` (공통 설정).
- **비즈니스 메트릭**: 주문 생성 시 `timedeal.order.create` Timer가 `result`·`errorCode` 태그와 함께 기록됨. 성능 분석·알람 설정 시 참고.

---

## 6. 로그

- **위치**: 표준 출력(stdout). 컨테이너/호스트에서 표준 로그 수집 도구로 수집하면 됨.
- **레벨**: `application.yml`·`application-perf.yml`의 `logging.level`로 조정. perf에서는 SQL 로그를 줄여두었음.
- **요청 로그**: `RequestLoggingFilter` 등으로 요청 URI·메서드 등을 남기도록 되어 있으면, 운영 시 로그 볼륨만 조절해 두면 됨.

---

## 7. 체크리스트 (배포 전)

- [ ] MySQL·Redis 접속 정보(호스트·포트·계정)를 환경에 맞게 설정 (`spring.datasource.*`, `spring.redis.*`)
- [ ] JWT `jwt.secret`을 운영용 값으로 교체 (환경변수 또는 시크릿 관리)
- [ ] `management.endpoints.web.exposure.include`를 운영에 필요한 범위로만 제한 (필요 시 `health`·`info`만 노출)
- [ ] 로그 레벨·로그 포맷을 운영 정책에 맞게 조정
- [ ] perf 사용 시 스키마(테이블·컬럼)가 앱과 일치하는지 확인 (`ddl-auto: validate`)

---

## 참고

- **실행 방법 요약**: [README 실행 방법](../README.md#실행-방법)
- **성능 테스트·프로파일**: [docs/perf/PERF_RESULT.md](perf/PERF_RESULT.md), [docs/perf/PERF_TEST_PLAN.md](perf/PERF_TEST_PLAN.md)
