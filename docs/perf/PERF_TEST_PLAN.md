## 타임딜 서비스 성능 테스트 계획서 (Performance Test Plan)

> **결과 정리**: 실측 결과는 [PERF_RESULT.md](PERF_RESULT.md)에서 **공통 조건(1절) → 시나리오 정의(2절) → Baseline(3절) → 실험별 비교(4절) → 부하 유형 비교(5절) → 요약·결론(6절)** 구조로 정리합니다.  
> 비교할 때는 **변수 하나만 바꾸고** 나머지를 공통 조건으로 두는 원칙을 따릅니다.

---

### 1. 목표

- **목적**
  - 타임딜 주문/재고/인증 기능이 **대규모 트래픽**에서도 안정적으로 동작하는지 검증한다.
  - 병목 지점을 찾아 **튜닝 전/후 수치를 비교**할 수 있는 자료를 만든다.
- **주요 성능 지표(SLI)**
  - **Latency**
    - p50, p90, p95 응답 시간 (ms)
  - **Error Rate**
    - HTTP 5xx 비율
    - 비즈니스 에러 (재고 부족, 타임딜 미오픈 등) 비율
  - **Throughput**
    - 초당 요청 수 (RPS)
    - 초당 주문 수
  - **Resource**
    - 애플리케이션: CPU, 메모리
    - DB: 커넥션 풀 사용량(Hikari), 슬로 쿼리
    - Redis: 커넥션/명령 수 (필요 시)

### 2. 목표 수준(SLO) 초안

> 실제 측정 후 조정 가능. 최초 목표값.

- **일반 트래픽 시나리오**
  - p95 응답 시간: **≤ 300ms**
  - 에러율(5xx): **< 0.5%**
  - RPS: **50~100 RPS 수준에서 안정적**
- **타임딜 오픈 피크 시나리오**
  - p95 응답 시간: **≤ 1s** (실측 B·C 기준으로 조정)
  - 에러율(5xx): **< 1%**
  - 주문 성공 시 재고/주문 데이터는 **정합성 100% (비관적 락으로 보장)**

> **실측 대비**: 현재 결과는 [PERF_RESULT.md](PERF_RESULT.md) **6절 SLO 대비**에 정리됨. 일반 트래픽(A·D)은 목표 충족. 피크(B·C)는 p95 목표를 1초로 조정하여 실측(≈860~960ms)과 맞춤, 5xx·정합성은 충족.

### 3. 테스트 환경

- **애플리케이션**
  - Spring Boot 4.0.2, Java 21
  - 프로파일: `local` (개발용), **`perf` (성능 테스트용, 추후 추가)**  
- **인프라**
  - MySQL 8.x, Redis 7.x
  - 로컬 Docker / `compose.yaml` 사용 (예: `docker compose up -d`)
  - DB/Redis 설정은 `application-perf.yml`에서 관리 (예정)
- **관찰도구**
  - Spring Boot Actuator
    - `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
  - P6Spy (SQL 로깅)
  - (선택) Prometheus + Grafana 연동

### 4. 테스트 데이터 & 전제 조건

- **테스트용 유저/아이템/재고**
  - 유저: 수백~수천 명 (예: 1000명)
  - 타임딜 아이템: 최소 3개
    - 각 아이템별 재고: 1000개 이상
  - 타임딜 오픈 시간(openTime): 성능 테스트 시점 기준으로 이미 오픈된 상태로 설정
- **전제 조건**
  - 애플리케이션이 `perf` 프로파일로 기동되어 있음.
  - DB, Redis가 정상적으로 동작하고 HealthIndicator가 `UP`.
  - Swagger/OpenAPI로 엔드포인트, 요청/응답 구조 확인 가능.

### 5. 성능 테스트 시나리오

#### 5.1 기본 트래픽 시나리오 (READ 중심)

- **목표**
  - 일반적인 서비스 사용 패턴에서의 성능(응답 시간, 에러율)을 측정.
- **대상 API**
  - `GET /api/items` (목록/검색)
  - `GET /api/items/{id}` (상세)
  - `POST /api/auth/login` → `GET /api/users/me` (인증 후 내 정보 조회)
- **부하 패턴 (예시)**
  - VUs: 0 → 50 (5분 ramp-up), 50 VUs로 5분 유지
- **k6 스크립트**
  - 파일명 예: `perf/k6/basic-read.js`

#### 5.2 타임딜 오픈 피크 시나리오 (주문/락 핵심)

- **목표**
  - 타임딜 오픈 직후 다수 사용자가 동시에 주문을 넣는 상황에서:
    - 재고/주문 정합성 보장 여부
    - 응답 시간, 에러율
  - 비관적 락이 실제로 어떤 동작/효과를 내는지 수치화.
- **대상 API**
  - `POST /api/auth/login`
  - `POST /api/orders` (타임딜 주문 생성)
- **부하 패턴 (예시)**
  - VUs: 0 → 200 (10초 내 급상승, spike)
  - 200 VUs로 1~2분 유지
- **중점 관찰**
  - 성공 주문 수 / 실패(재고 부족, 타임딜 미오픈 등) 비율
  - DB 락 대기 시간, 커넥션 풀 사용량
- **k6 스크립트**
  - 파일명 예: `perf/k6/order-spike.js`

#### 5.3 장시간 Soak 테스트 (안정성)

- **목표**
  - 낮은/중간 수준의 RPS로 장시간(1~2시간) 운영 시:
    - 메모리 릭, 커넥션 누수, GC 문제 여부 검증.
- **대상 API**
  - 기본 트래픽 + 주문 API 일부 포함.
- **부하 패턴 (예시)**
  - VUs: 10~30 수준으로 1~2시간 유지.
- **k6 스크립트**
  - 파일명 예: `perf/k6/soak-mixed.js`

#### 5.4 장애/에러 시나리오 (선택)

- **목표**
  - DB/Redis 장애 혹은 지연 상황에서:
    - API가 어떤 에러를 반환하는지,
    - 사용자가 어떤 UX를 경험하는지,
    - 시스템이 어떻게 복구되는지 확인.
- **실행 절차 (예시)**
  1. 애플리케이션·DB·Redis 정상 기동 후 `k6 run perf/k6/basic-read.js` 등으로 **정상 Baseline** 1회 기록.
  2. **DB 일시 중단**: `docker stop <mysql 컨테이너>` 후 동일 k6 스크립트 실행 → 에러 유형·비율·응답 코드 기록. 복구: `docker start <mysql 컨테이너>` 후 health 확인, k6 재실행하여 복구 후 동작 확인.
  3. **Redis 일시 중단**: `docker stop <redis 컨테이너>` 후 로그인·주문 포함 스크립트(예: `order-spike.js`) 실행 → JWT 블랙리스트·세션 등 Redis 의존 API의 에러·동작 기록. 복구 후 동일하게 재실행.
  4. (선택) DB/Redis 지연: `tc` 등으로 지연 부여 후 동일 스크립트 실행, 레이턴시·에러율 변화 기록.
- **결과 기록**
  - [PERF_RESULT.md](PERF_RESULT.md) **7절 장애/에러 시나리오**에 상황별 표(에러 유형, http_req_failed, 복구 후 성공 여부)로 정리.

#### 5.5 상품 분산 주문 시나리오 (order-spike-distributed)

- **목표**
  - 5.2(order-spike)는 **동일 아이템에 주문이 몰리는** 패턴.  
    이 시나리오는 **목록에서 여러 아이템을 골라 분산 주문**하여, “한 상품 몰림 vs 분산” 시 처리량·에러율·락 경합 차이를 비교한다.
- **대상 API**
  - `GET /api/items` (목록에서 아이템 ID 선택)
  - `POST /api/auth/login`
  - `POST /api/orders` (선택된 itemId로 주문)
- **부하 패턴**
  - order-spike와 동일하게 0→200 VU(10s), 200 VU 유지(60s) 등.  
    환경변수 `RAMP_UP`, `RAMP_TARGET`, `HOLD_DURATION`로 조정 가능.
- **k6 스크립트**
  - `perf/k6/order-spike-distributed.js`

### 6. k6 구조 및 실행 방법 (초안)

- **디렉토리 구조 제안**
  - `perf/k6/basic-read.js`
  - `perf/k6/order-spike.js`
  - `perf/k6/order-spike-distributed.js` (상품 분산 주문)
  - `perf/k6/soak-mixed.js`
  - `perf/k6/lib/auth.js` (로그인 및 토큰 발급 유틸)
  - `perf/k6/lib/config.js` (BASE_URL, 기본 옵션 등 공통 설정)

- **공통 실행 방법**
  - 애플리케이션 기동:
    - `./gradlew bootRun --args='--spring.profiles.active=perf'`
  - k6 실행 예시:
    - `k6 run perf/k6/basic-read.js`
    - `k6 run perf/k6/order-spike.js`
    - `k6 run perf/k6/order-spike-distributed.js` (환경변수: TEST_EMAIL, TEST_PASSWORD 등)

### 7. 측정/분석 방법

- **k6 결과**
  - 기본 summary: Latency(p50/p90/p95), RPS, 에러율.
  - 필요 시 `--out json=./perf/results/<name>.json` 으로 저장 후 그래프화.
- **애플리케이션/DB 메트릭**
  - Actuator:
    - `/actuator/metrics/http.server.requests`
    - `/actuator/metrics/hikaricp.connections.active`
  - P6Spy:
    - 슬로 쿼리 패턴, 락 대기 쿼리 확인.

- **결과 문서(PERF_RESULT) 구조**
  - **0절**: 한 페이지 요약 — 공통 조건·시나리오·실험·Baseline 위치
  - **1절**: 공통 조건 1회 정의 — 환경·앱 설정(풀·캐시·락·인덱스)·시나리오별 데이터 조건
  - **2절**: 부하 시나리오 정의 — A·B·C·D의 스크립트·부하·대상 API만, 수치 없음  
    (A=5.1 basic-read, B=5.2 order-spike, C=5.5 order-spike-distributed, D=5.3 soak-mixed)
  - **3절**: Baseline 결과 — 1·2절 조건으로 A·B·C·D 각각 측정한 한 표
  - **4절**: 실험별 비교 — 재고 수준·Hikari 풀·캐시·락 전략 등, **각 실험마다 "바꾼 것 / 나머지 1·2절과 동일"** 명시 후 표·해석
  - **5절**: 부하 유형 비교 — order-spike(B) vs order-spike-distributed(C). 요청 구성이 다르므로 "동일 조건 실험"이 아님을 명시
  - **6절**: 요약·결론(SLO 대비 소절 포함), **7절**: 장애/에러 시나리오(선택, 기록 구조), **부록**: 실행 예시·사전 준비 SQL·인덱스

### 8. 향후 개선 아이디어 (포트폴리오용)

- 비관적 락 외에:
  - 낙관적 락, 분산 락(Redis 기반)과의 비교 정리.
  - 락 타임아웃, 재시도 전략 설계.
- 안정성·레질리언스:
  - Rate limiting, Circuit breaker(Resilience4j 등) 적용 후 성능/안정성 비교.
- 관찰성:
  - Prometheus + Grafana 대시보드 스크린샷을 포트폴리오에 포함.

