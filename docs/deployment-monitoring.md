# 배포·모니터링 가이드

로컬/성능 테스트용으로 앱을 띄우는 방법과, 헬스·메트릭·로그를 보는 방법을 한 페이지로 정리합니다.

---

## 1. 사전 요구 사항

- **Java 21**, **Gradle 8.x** (또는 `./gradlew` 사용)
- **Docker**, **Docker Compose** (MySQL·Redis용)

---

## 2. 인프라 기동 (MySQL, Redis, Prometheus, Grafana)

```bash
docker compose up -d
```

- **MySQL**: `localhost:3306`, DB `timedeal`, 사용자 `timedeal` / `timedeal`
- **Redis**: `localhost:6379`
- **Prometheus**: `localhost:9090` (앱 메트릭 수집, 앱은 호스트에서 별도 실행)
- **Grafana**: `localhost:3000` (대시보드, 기본 로그인 admin / admin)
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

| 목적                    | 프로파일                   | 실행 예시                                                                  |
| ----------------------- | -------------------------- | -------------------------------------------------------------------------- |
| 로컬 개발               | `local`                    | `./gradlew bootRun` (또는 `--args='--spring.profiles.active=local'`)       |
| 성능 테스트             | `perf`                     | `./gradlew bootRun --args='--spring.profiles.active=perf'`                 |
| 성능 테스트 (낙관적 락) | `perf` + `perf-optimistic` | `./gradlew bootRun --args='--spring.profiles.active=perf,perf-optimistic'` |

- **perf** 사용 시: `ddl-auto: validate`이므로 스키마는 미리 준비해 두어야 함. 필요 시 DB에 `stocks.version` 등 DDL 적용 후 기동.
- **perf-optimistic**: `order.lock-strategy: optimistic`만 덮어쓰므로, `perf`와 함께 사용.

---

## 4. 헬스 체크

| 엔드포인트             | 용도                                    |
| ---------------------- | --------------------------------------- |
| `GET /actuator/health` | 서버 up/down 및 DB·Redis 등 의존성 상태 |

- **형식**: JSON. `status`: `UP` / `DOWN`, `components`에 db·redis 등 상세 포함.
- **설정**: `management.endpoint.health.show-details`에 따라 상세 노출 여부 달라짐 (기본 `when-authorized`).

배포 환경에서는 로드밸런서·컨테이너 오케스트레이터에서 이 URL을 주기적으로 호출해 정상 여부(살아 있는지) 판단에 사용하면 된다.

---

## 5. 메트릭·Prometheus

| 엔드포인트                     | 용도                                                            |
| ------------------------------ | --------------------------------------------------------------- |
| `GET /actuator/metrics`        | 사용 가능한 메트릭 이름 목록                                    |
| `GET /actuator/metrics/{name}` | 특정 메트릭 값 (예: `jvm.memory.used`, `timedeal.order.create`) |
| `GET /actuator/prometheus`     | Prometheus 스크래핑용 텍스트 형식                               |

- **노출 메트릭**: `health`, `info`, `metrics`, `prometheus` (공통 설정).
- **비즈니스 메트릭**: 주문 생성 시 `timedeal.order.create` Timer가 `result`·`errorCode` 태그와 함께 기록됨. 성능 분석·알람 설정 시 참고.

---

## 6. Prometheus + Grafana 구성

로컬에서 메트릭 수집·대시보드를 보려면 **앱(호스트) + MySQL/Redis + Prometheus + Grafana**를 모두 띄운 뒤 Grafana에서 확인한다.

### 6.1 기동 순서

1. **인프라 + 모니터링 스택 기동** (프로젝트 루트)

   ```bash
   docker compose up -d
   ```

   - MySQL(3306), Redis(6379), **Prometheus(9090)**, **Grafana(3000)** 이 올라간다.

2. **애플리케이션 실행** (호스트에서)

   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=perf'
   ```

   - Prometheus는 `host.docker.internal:8080`의 `/actuator/prometheus`를 15초마다 스크래핑한다.

3. **Grafana 접속**
   - URL: http://localhost:3000
   - 로그인: **admin** / **admin** (초기 비밀번호, 접속 후 변경 권장)

### 6.2 Prometheus

| 항목 | 내용                                                                                |
| ---- | ----------------------------------------------------------------------------------- |
| URL  | http://localhost:9090                                                               |
| 설정 | `monitoring/prometheus/prometheus.yml` (스크래핑 대상: `host.docker.internal:8080`) |
| 확인 | Status → Targets에서 `timedeal-app`이 UP이면 정상 수집 중                           |

- Linux에서 `host.docker.internal`이 동작하지 않으면, compose의 `prometheus.extra_hosts`에 `host.docker.internal:host-gateway`가 설정되어 있는지 확인한다.

### 6.3 Grafana 사용법

#### 접속·로그인

1. 브라우저에서 **http://localhost:3000** 접속
2. 로그인: 사용자 **admin**, 비밀번호 **admin**
3. 첫 로그인 시 비밀번호 변경 화면이 나오면 **Skip** 하거나 새 비밀번호로 변경

#### 대시보드 열기

1. 왼쪽 사이드바(☰)에서 **Dashboards** 클릭
2. **Browse** → **Timedeal** 폴더 선택
3. **Timedeal 서비스** 대시보드 클릭

또는 상단 검색창에 "Timedeal" 입력 후 **Timedeal 서비스** 선택.

#### 대시보드에서 할 수 있는 것

| 패널                                     | 보여주는 것                                                                                   |
| ---------------------------------------- | --------------------------------------------------------------------------------------------- |
| **HTTP 요청률 (RPS, URI별)**             | API 경로별 초당 요청 수. 부하 테스트 시 어떤 URI가 많이 호출되는지 확인                       |
| **주문 생성 호출률 (result별)**          | 주문 API가 success / business_error / failure 로 얼마나 나뉘는지 (재고 부족·타임딜 미오픈 등) |
| **주문 생성 응답 시간 (평균, result별)** | 주문 처리에 걸린 평균 시간(초). 스파이크 시 레이턴시 상승 확인                                |
| **JVM 메모리 사용량**                    | 힙·메타스페이스 등 메모리 사용량. 장시간 부하 시 메모리 증가 추이 확인                        |

- **시간 범위**: 우측 상단에서 **Last 1 hour**, **Last 5 minutes** 등으로 구간 변경 가능
- **새로고침**: 우측 상단 새로고침 버튼 또는 자동 새로고침(기본 10초)으로 최신 데이터 반영

#### k6 부하 테스트와 함께 쓰기

1. 앱(`perf` 프로파일) + Prometheus + Grafana가 떠 있는 상태에서
2. Grafana에서 **Timedeal 서비스** 대시보드 열어 두고
3. 다른 터미널에서 k6 실행 (예: `k6 run perf/k6/order-spike.js`)
4. 대시보드에서 RPS 상승, 주문 성공/실패 비율, 응답 시간·메모리 추이를 실시간으로 확인

포트폴리오용으로 **대시보드 스크린샷**을 찍어 PERF_RESULT.md 또는 README에 넣으면 관찰성 구성을 보여주기 좋다.

### 6.4 "No data"가 나올 때 (트러블슈팅)

Grafana에서 **No data**가 뜨면 아래 순서로 확인한다.

1. **Prometheus가 앱을 스크래핑하는지**
   - 브라우저에서 http://localhost:9090 → **Status** → **Targets** 이동
   - **timedeal-app** 타깃이 **UP**인지 확인
   - **DOWN**이면: 앱이 8080에서 떠 있는지, **perf** 프로파일로 실행했는지 확인한 뒤, 터미널에서 `curl http://localhost:8080/actuator/prometheus` 로 메트릭이 보이는지 확인

2. **앱에서 메트릭이 나오는지**
   - 브라우저에서 http://localhost:8080/actuator/prometheus 접속
   - `http_server_requests` 또는 `jvm_memory_used` 같은 줄이 보이면 정상
   - 404/연결 실패면: `management.endpoints.web.exposure.include`에 `prometheus`가 들어 있는지 확인 (기본·perf 설정에는 포함됨)

3. **Grafana 시간 범위**
   - 우측 상단에서 **Last 15 minutes** 또는 **Last 1 hour** 로 넓혀서 다시 확인
   - 방금 스크래핑을 시작했다면 15초~1분 정도 지난 뒤에 그래프가 찍힌다

4. **Prometheus에서 실제 메트릭 이름 확인**
   - Grafana 왼쪽 메뉴 **Explore** → 데이터소스 **Prometheus** 선택
   - 쿼리에 `{job="timedeal-app"}` 또는 `http_server_requests` 입력 후 **Run query**
   - 시리즈가 나오면 job·메트릭 이름이 맞는 것이고, 대시보드에서만 안 보일 수 있음(시간 범위·패널 쿼리 재확인)
   - Spring Boot 버전에 따라 `http_server_requests_seconds_count` 대신 `http_server_requests_duration_seconds_count` 로 나올 수 있음. 이 경우 대시보드 패널의 쿼리를 해당 이름으로 바꾸거나, 아래 6.5절 참고

5. **Docker에서 호스트 접속 (Mac/Windows 외)**
   - Linux 등에서 `host.docker.internal`이 동작하지 않으면, `compose.yaml`의 **prometheus** 서비스에 `extra_hosts: - "host.docker.internal:host-gateway"` 가 있는지 확인 (이미 포함됨)

### 6.5 설정 파일 위치

| 구성요소           | 경로                                                                      |
| ------------------ | ------------------------------------------------------------------------- |
| Prometheus 설정    | `monitoring/prometheus/prometheus.yml`                                    |
| Grafana 데이터소스 | `monitoring/grafana/provisioning/datasources/datasource.yml`              |
| Grafana 대시보드   | `monitoring/grafana/provisioning/dashboards/json/timedeal-dashboard.json` |

---

## 7. 로그

- **위치**: 표준 출력(stdout). 컨테이너/호스트에서 표준 로그 수집 도구로 수집하면 됨.
- **레벨**: `application.yml`·`application-perf.yml`의 `logging.level`로 조정. perf에서는 SQL 로그를 줄여두었음.
- **요청 로그**: `RequestLoggingFilter` 등으로 요청 URI·메서드 등을 남기도록 되어 있으면, 운영 시 로그 볼륨만 조절해 두면 됨.

---

## 8. 체크리스트 (배포 전)

- [ ] MySQL·Redis 접속 정보(호스트·포트·계정)를 환경에 맞게 설정 (`spring.datasource.*`, `spring.redis.*`)
- [ ] JWT `jwt.secret`을 운영용 값으로 교체 (환경변수 또는 시크릿 관리)
- [ ] `management.endpoints.web.exposure.include`를 운영에 필요한 범위로만 제한 (필요 시 `health`·`info`만 노출)
- [ ] 로그 레벨·로그 포맷을 운영 정책에 맞게 조정
- [ ] perf 사용 시 스키마(테이블·컬럼)가 앱과 일치하는지 확인 (`ddl-auto: validate`)

---

## 참고

- **실행 방법 요약**: [README 실행 방법](../README.md#실행-방법)
- **성능 테스트·프로파일**: [docs/perf/PERF_RESULT.md](perf/PERF_RESULT.md), [docs/perf/PERF_TEST_PLAN.md](perf/PERF_TEST_PLAN.md)
