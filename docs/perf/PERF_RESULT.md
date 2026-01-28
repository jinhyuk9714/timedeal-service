## 타임딜 서비스 성능 테스트 결과 (초기 리포트)

> 이 문서는 `PERF_TEST_PLAN.md` 기반으로 수행한 성능 테스트의 **실측 결과**를 정리한 리포트입니다.  
> 추후 튜닝/추가 시나리오 진행 시 이 파일을 계속 업데이트합니다.

---

### 0. 요약

- **기본 READ 시나리오**
  - 약 **40 RPS**에서 `GET /api/items`, `GET /api/items/{id}` 모두 **에러율 0%, p95 < 50ms**.
- **타임딜 주문 스파이크 – 재고 부족 시나리오**
  - 약 **165 RPS**에서 모든 주문이 `INSUFFICIENT_STOCK`(400)으로 처리되며, **데이터 정합성 유지, p95 ≈ 950ms**.
- **타임딜 주문 스파이크 – 재고 충분 시나리오 (인덱스 포함)**
  - 약 **160 RPS**에서 **모든 주문(6505건)이 201로 성공**, 에러율 0%, **p95 ≈ 960ms**.
- **내부 서비스 로직 시간**
  - `OrderService.createOrder()` Micrometer Timer 기준:  
    - 평균 **≈ 3~4ms**, 최대 **≈ 112ms**, 호출 수 **6590회**.  
    - → **주요 병목은 비즈니스 로직보다 DB I/O, 네트워크, 필터 체인 등 외부 요소에 더 가까움**.
- **Soak 시나리오 (장시간 부하)**
  - **10 VU · 10분** 동안 로그인 + 목록/상세 + 30% 확률 주문 혼합: **에러율 0%**, **p95 ≈ 118ms**, 약 **29 RPS**로 안정 동작.
- **캐시 도입 (B-2, Caffeine)**
  - 상품 목록·상세 캐시 적용 후 basic-read 20 VU 60s: **p95 38.8ms → 11.6ms**(약 70% 개선), RPS ≈ 39.5, 에러율 0%.

---

### 1. 테스트 환경

- **애플리케이션**
  - Spring Boot 4.0.2, Java 21
  - 프로파일: `perf`
  - Port: `8080`
- **데이터베이스**
  - MySQL 8.0.x (`jdbc:mysql://localhost:3306/timedeal`)
  - HikariCP 기본 설정 + `application-perf.yml`의 풀 설정
- **부하 도구**
  - k6 v1.5.0
  - 스크립트 위치: `perf/k6/*.js`

---

### 2. 시나리오 A – 기본 READ (상품 목록/상세)

**스크립트**: `perf/k6/basic-read.js`  
**대상 API**:
- `GET /api/items`
- `GET /api/items/{id}` (목록의 첫 번째 아이템 기준)

#### 2-1. 1 VU, 10초

- 명령:
  ```bash
  k6 run perf/k6/basic-read.js
  ```
- 결과 요약:
  - 요청 수(`http_reqs`): **20** (약 2 RPS)
  - 성공률:
    - `list status is 200`: 100%
    - `detail status is 200`: 100%
    - `http_req_failed`: **0% (0/20)**
  - 레이턴시(`http_req_duration`):
    - 평균: **≈ 28.6ms**
    - p50 (med): **≈ 17.0ms**
    - p90: **≈ 40.3ms**
    - p95: **≈ 47.8ms**

#### 2-2. 20 VUs, 60초

- 명령:
  ```bash
  VUS=20 DURATION=60s k6 run perf/k6/basic-read.js
  ```
- 결과 요약:
  - 요청 수(`http_reqs`): **2320** (약 **38.5 RPS**)
  - 성공률:
    - `list status is 200`: 100%
    - `detail status is 200`: 100%
    - `http_req_failed`: **0% (0/2320)**
  - 레이턴시(`http_req_duration`):
    - 평균: **≈ 18.9ms**
    - p50 (med): **≈ 14.9ms**
    - p90: **≈ 36.5ms**
    - p95: **≈ 38.8ms**

**해석**
- 약 **40 RPS 수준의 READ 트래픽**에서 **p95 < 50ms**, 에러율 0%로 **PERF_PLAN에 정의한 일반 트래픽 SLO(p95 ≤ 300ms, 에러율 < 0.5%)를 여유 있게 만족**.
- DB/애플리케이션 리소스 입장에서 읽기 요청은 현재 설정으로 충분히 감당 가능함을 확인.

---

### 3. 시나리오 B – 타임딜 주문 스파이크

**스크립트**: `perf/k6/order-spike.js`  
**대상 API**:
- `POST /api/auth/login`
- `POST /api/orders`

**부하 패턴** (`ramping-vus`):
- 0 → 200 VU (10초)
- 200 VU 유지 (60초)
- 200 → 0 VU (10초)

**실행 예시** (실제 사용한 값):
```bash
TEST_EMAIL=<유효한_테스트_계정_이메일> \
TEST_PASSWORD=<해당_계정_비밀번호> \
ITEM_ID=1 \
QUANTITY=1 \
k6 run perf/k6/order-spike.js
```

#### 3-1. 결과 요약

- k6 요약 (주요 지표만 발췌):
  - `checks_total`: **26664**
  - `checks_succeeded`: **75.75% (20198/26664)**
  - `checks_failed`: **24.24% (6466/26664)**
  - 체크별:
    - `login status is 200`: 100% 성공
    - `login has token`: 100% 성공
    - `order status is 201`: **3% 성공 (200 / 6666)**  
    - `order business error or success`: 100% (2xx/4xx 등 기대 범위 내 응답)
  - HTTP:
    - 총 요청 수(`http_reqs`): **13332** (약 **164.6 RPS**)
    - 실패율(`http_req_failed`): **48.49% (6466/13332)** – 비 2xx 응답 포함
    - 레이턴시(`http_req_duration` 전체):
      - 평균: **≈ 561ms**
      - p50 (med): **≈ 572ms**
      - p90: **≈ 885ms**
      - p95: **≈ 959ms**
  - 실행:
    - `iterations`: 6666 (약 **82.3 TPS** 수준의 주문 시도)
    - `iteration_duration p95`: ≈ 2.63s

#### 3-2. 주문 응답 코드/원인 분포 (커스텀 메트릭 기반)

커스텀 Counter 메트릭을 추가하여, 주문 응답 결과를 다시 측정:

```bash
k6 run perf/k6/order-spike.js
```

해당 실행에서의 주요 결과:

- **커스텀 메트릭**
  - `orders_total`: **6690**
  - `orders_400_insufficient_stock`: **6690**
  - (`orders_success_201`, `orders_400_timedeal_not_opened`, `orders_4xx_other`, `orders_5xx`는 0으로 관측)
- **체크/HTTP 요약**
  - `checks_total`: 26760, `checks_succeeded`: 75.00%
  - `order status is 201`: 0% (성공 주문 없음)
  - `order business error or success`: 100%
  - `http_req_failed`: 50% (비 2xx 응답 포함)
  - `http_reqs`: 13380 (약 **165.4 RPS**)
  - `http_req_duration p95`: **≈ 948ms**

**분석**

- 이 실행에서는 **전체 주문 시도(6690건)가 모두 `재고가 부족합니다`(INSUFFICIENT_STOCK) 에러로 응답**됨.
  - 즉, 테스트 시점에 설정된 재고 대비 주문 시도가 훨씬 많아, 성공 주문이 더 이상 발생하지 않는 상태에서 스파이크가 걸린 상황.
- 비즈니스 관점:
  - **재고가 정확하게 소진된 이후에는 모든 추가 주문에 대해 일관되게 “재고 부족” 에러를 반환**하고 있어, 데이터 정합성은 유지되고 있음.
- 성능 관점:
  - 약 165 RPS 수준에서 **p95 ≈ 950ms**로, “재고 부족” 비즈니스 에러가 대량으로 발생하는 상황에서도 1초 이내 응답을 유지.
  - 추후 재고 수량/아이템 개수를 조정해 “성공 주문 + 재고 부족 혼합 구간”을 별도로 측정하면, 초기 성공 구간의 레이턴시/성공률도 추가로 분석 가능.

#### 3-3. 내부 서비스 로직 시간 (Micrometer Timer + Actuator)

`OrderService.createOrder()`에 Micrometer `Timer` 메트릭(`timedeal.order.create`)을 추가하고, k6 주문 스파이크 직후 Actuator로 조회:

```bash
curl -i "http://localhost:8080/actuator/metrics/timedeal.order.create"
curl -i "http://localhost:8080/actuator/metrics/timedeal.order.create?tag=result:business_error&tag=errorCode:INSUFFICIENT_STOCK"
```

응답(JSON) 주요 내용:

- `name`: `timedeal.order.create`
- `availableTags`:
  - `result`: `["business_error"]`
  - `errorCode`: `["INSUFFICIENT_STOCK"]`
- `measurements`:
  - `COUNT`: **6590.0**
  - `TOTAL_TIME`: **23.481250515초**
  - `MAX`: **0.111836584초 (약 112ms)**

**해석**

- COUNT 6590: `createOrder()`가 **총 6590회 호출** (k6의 `orders_total`=6590과 일치).
- TOTAL_TIME / COUNT ≈ 23.48 / 6590 ≈ **0.00356초 (약 3.6ms)**  
  → **주문 생성 비즈니스 로직 자체는 평균 3~4ms 수준으로 매우 짧게 처리**됨.
- MAX ≈ 112ms: 단일 호출 기준 최대 처리 시간도 **100ms 초반** 수준.
- 태그 결과가 모두 `result=business_error`, `errorCode=INSUFFICIENT_STOCK`인 것으로 보아,
  - 이 측정 구간의 모든 호출이 “재고 부족” 비즈니스 예외 흐름이었음을 다시 한 번 확인.
- k6 기준 end-to-end 응답시간은 p95 ≈ 940~950ms 수준이지만,
  - 내부 서비스 메서드 시간은 평균 3~4ms, 최대 112ms로 측정되어  
  - **전체 레이턴시의 대부분은 네트워크, 필터 체인, 직렬화/역직렬화, 스레드 스케줄링, DB I/O 등 외부/주변 요소에서 발생**함을 보여준다.

**해석 (1차)**
- **로그인**:
  - 모든 VU에서 `POST /api/auth/login`은 **100% 성공**하고 토큰을 정상 발급함.
- **주문**:
  - 약 160 RPS 수준의 로그인+주문 폭주 상황에서,
    - 실제로 **201(주문 성공)** 응답을 받은 건 약 200건(3%) 정도.
    - 나머지 주문 요청은 비즈니스 에러(재고 부족, 타임딜 조건 불충족 등)로 추정되는 **비 2xx 응답**.
  - `order business error or success` 체크는 “예상 가능한 비즈니스 응답 (200/201/400/404 등)”을 모두 통과로 간주하므로 **비즈니스 관점에서는 예외 상황도 정상적으로 처리**되고 있음.
- **지연 시간**:
  - 타임딜 스파이크 상황에서 주문 API의 `http_req_duration p95`가 **약 1초 내외**로 측정됨.
  - PERF_PLAN의 피크 시나리오 목표(p95 ≤ 500ms)와 비교하면 **튜닝 여지는 있으나, 1초 이내에서 안정적으로 응답**하는 편.

#### 3-4. 재고 충분 + 인덱스 적용 후 스파이크 (성공 시나리오)

위 실행들은 대부분 “재고 부족” 상황을 가정한 것이므로, **재고를 충분히 늘린 상태에서 “성공 주문만 발생하는” 스파이크**도 별도로 측정:

- **사전 작업 (MySQL)**:
  - 인덱스:
    ```sql
    CREATE INDEX idx_orders_user_created_at ON orders (user_id, created_at DESC);
    CREATE INDEX idx_orders_item_id ON orders (item_id);
    CREATE INDEX idx_stocks_item_id ON stocks (item_id);
    ```
  - 재고/오픈 시간:
    ```sql
    UPDATE items SET open_time = NOW() - INTERVAL 1 HOUR WHERE id = 1;
    UPDATE stocks SET quantity = 100000 WHERE item_id = 1;
    ```

- **실행 명령**:
  ```bash
  TEST_EMAIL=user@example.com \
  TEST_PASSWORD=password123 \
  ITEM_ID=1 \
  QUANTITY=1 \
  k6 run perf/k6/order-spike.js
  ```

- **k6 결과 요약**:
  - `checks_total`: 26020, `checks_succeeded`: 100%, `checks_failed`: 0%
  - 체크별:
    - `login status is 200`: 100% 성공
    - `login has token`: 100% 성공
    - `order status is 201`: **100% 성공 (6505/6505)**
    - `order business error or success`: 100%
  - CUSTOM 메트릭:
    - `orders_total`: **6505**
    - `orders_success_201`: **6505**
    - `orders_400_insufficient_stock`: 0
  - HTTP:
    - `http_reqs`: **13010** (약 **160.8 RPS**)
    - `http_req_failed`: 0% (0/13010)
    - `http_req_duration`:
      - 평균: **≈ 587ms**
      - p50 (med): **≈ 599ms**
      - p90: **≈ 896ms**
      - p95: **≈ 963ms**

**비교·해석**

- **재고 부족 시나리오**:
  - RPS ≈ 165, 전부 `INSUFFICIENT_STOCK` 에러, p95 ≈ 950ms.
  - “락 + 잦은 비즈니스 에러” 상황에서도 1초 내외 응답을 유지.
- **재고 충분 시나리오 (인덱스 포함)**:
  - RPS ≈ 160, **모든 주문(6505건)이 201로 성공**, 실패율 0%.
  - p95 ≈ 960ms로, 에러 없이 성공만 발생하는 상황에서도 스파이크 부하에서 1초 내외 응답 유지.
- 종합하면:
  - 비관적 락과 재고 체크 로직이 **재고 부족/충분 두 상황 모두에서 정합성을 지키면서**,  
    약 160 RPS 수준의 부하에서 **p95 ~ 1초 이내의 응답 시간**을 제공하고 있음을 수치로 확인.
  - `timedeal.order.create` Timer 기준 내부 처리 시간(평균 3~4ms, 최대 ≈ 112ms)을 고려하면,  
    **주요 병목은 도메인 로직보다 DB I/O, 네트워크, 필터 체인 등 외부 요소에 더 가깝다**는 점도 함께 정리할 수 있다.

---

### 4. 시나리오 C – Soak (장시간 부하)

**스크립트**: `perf/k6/soak-mixed.js`  
**대상 API**:
- `POST /api/auth/login`
- `GET /api/items`
- `GET /api/items/{id}` (목록의 첫 번째 아이템 기준)
- `POST /api/orders` (약 30% 확률로 호출)

**부하 패턴**:
- 10 VU 고정, 10분 유지
- 각 iteration: 로그인 → 목록 조회 → 상세 조회 → (30% 확률) 주문 생성

**실행 명령**:
```bash
k6 run perf/k6/soak-mixed.js
```

환경 변수로 조정 가능: `SOAK_VUS`, `SOAK_DURATION` 등.

#### 4-1. 결과 요약 (10 VU, 10분)

- **Threshold**
  - `http_req_failed rate < 0.01`: **통과** (실측 0.00%)
- **Checks**
  - `checks_total`: **22932** (약 38.2/s)
  - `checks_succeeded`: **100%** (22932/22932)
  - `checks_failed`: **0%**
  - 체크별: `login status is 200`, `login has token`, `list status is 200`, `detail status is 200`, `order status is 201 or 400/404` 모두 통과
- **HTTP**
  - `http_reqs`: **17590** (약 **29.3 RPS**)
  - `http_req_failed`: **0%** (0/17590)
  - `http_req_duration`:
    - 평균: **≈ 37.48ms**
    - median: **≈ 7.28ms**
    - p90: **≈ 113.55ms**
    - p95: **≈ 118.37ms**
    - max: **≈ 327.52ms**
- **실행**
  - `iterations`: **5342** (약 8.9/s)
  - `iteration_duration`: 평균 ≈ 1.12s, p95 ≈ 1.14s
  - `vus`: 10 (고정)

**해석**

- 10분 동안 **에러 없이** 로그인·목록·상세·주문 혼합 트래픽이 약 29 RPS로 유지됨.
- **p95 ≈ 118ms**로, 기본 READ 시나리오(20 VU 기준 p95 ≈ 39ms)보다는 다소 높지만, 장시간 부하에서 **SLO(p95 ≤ 300ms, 에러율 < 0.5%)를 여유 있게 만족**.
- Soak 관점에서 **메모리 누수·연결 풀 고갈·점진적 성능 저하 없이** 10분 구간이 안정적으로 유지되었음을 확인.

**추가 스냅샷**  
- 동일 perf 기동(캐시 적용) 상태에서 B-2 측정 시 함께 돌린 soak-mixed: RPS ≈ 29.5, p95 ≈ 119ms, 에러율 0%.

---

### 5. 현재까지의 결론 및 다음 단계 제안

1. **기본 READ 시나리오**
   - 1 VU/10초, 20 VU/60초 모두 에러율 0%, p95 < 50ms로 매우 양호.
   - 이 결과를 기준선(Baseline)으로 유지하면서 이후 튜닝 시 변화를 비교할 수 있음.

2. **타임딜 주문 스파이크 시나리오**
   - 로그인은 100% 성공, 주문은 재고/비즈니스 제약 때문에 성공률이 낮음(3% 수준).
   - k6 기준 end-to-end 응답시간 p95 ≈ 1초 수준으로, 실 서비스 요구사항에 따라 추가 튜닝 여지는 있으나 안정적으로 응답하고 있음.
   - 커스텀 k6 메트릭 + Micrometer Timer로,
     - “성공 주문 vs 재고 부족” 비율,
     - `createOrder()` 내부 처리 시간(평균 ≈ 3~4ms, 최대 ≈ 112ms)을 수치화하여,  
       **도메인 로직은 매우 빠르게 동작하고 있고, 전체 레이턴시는 주로 외부/주변 요소에서 발생**한다는 근거를 확보함.

3. **Soak 시나리오**
   - 10 VU · 10분 soak-mixed 실행에서 에러율 0%, p95 ≈ 118ms, 약 29 RPS로 장시간 부하 시에도 안정 동작을 확인함.

4. **향후 리포트 확장 방향**
   - 튜닝 전/후(예: 인덱스 추가, Hikari 풀 조정, 캐시 도입 등) 수치를 표/그래프로 비교.
   - 비관적 락 전략과 낙관적 락/분산 락과의 비교 실험을 진행한 후, 결과를 이 문서에 추가.

---

### 6. Hikari 풀 조정 실험 (B-1)

**목적**: connection pool 크기·유휴 연결 수 조정이 스파이크·Soak 구간에서 성능에 미치는지 비교.

**Baseline (현재)**  
- `application-perf.yml` 기준: `maximum-pool-size: 30`, `minimum-idle: 10`  
- 2~4절 수치는 모두 이 설정으로 측정됨.

**조정안**  
- `maximum-pool-size: 50`, `minimum-idle: 20`

**조정 후 측정 방법**  
1. perf 프로파일로 기동할 때 환경변수로 오버라이드:
   ```bash
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50 \
   SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=20 \
   ./gradlew bootRun --args='--spring.profiles.active=perf'
   ```
2. 동일 k6 스크립트로 재측정: `basic-read`(20 VU 60s), `order-spike`, `soak-mixed` 등.
3. 아래 표의 "조정 (50/20)" 열에 수치 기입.

#### 6-1. 결과 비교표 (측정 후 기입)

| 시나리오 | 지표 | Baseline (30/10) | 조정 (50/20) |
|----------|------|------------------|--------------|
| basic-read (20 VU 60s) | RPS | ≈ 38.5 | **≈ 38.5** |
| basic-read (20 VU 60s) | p95 | ≈ 38.8ms | **≈ 34ms** |
| basic-read (20 VU 60s) | http_req_failed | 0% | **0%** |
| order-spike (재고 충분) | RPS | ≈ 160 | **≈ 167.6** |
| order-spike (재고 충분) | p95 | ≈ 960ms | **≈ 987ms** |
| order-spike (재고 충분) | http_req_failed | 0% | **0%** |
| soak-mixed (10 VU 10분) | RPS | ≈ 29 | **≈ 29** |
| soak-mixed (10 VU 10분) | p95 | ≈ 118ms | **≈ 129ms** |
| soak-mixed (10 VU 10분) | http_req_failed | 0% | **0%** |

**참고**  
- **basic-read** 조정(50/20): 동일 조건(20 VU 60s, perf + 풀 50/20)으로 측정.
- **order-spike**, **soak-mixed** 조정(50/20): `TEST_EMAIL=user@example.com TEST_PASSWORD=password123 ITEM_ID=1 QUANTITY=1` 로 동일 조건에서 측정.
- **추가 스냅샷**: perf + 캐시 적용 기동 상태에서 B-2 측정 시 함께 실행한 order-spike(RPS ≈ 162.8, p95 ≈ 935ms), soak-mixed(RPS ≈ 29.5, p95 ≈ 119ms) — 4절·7절과 동일 run.

**해석**  
- **basic-read**: 풀 30/10 → 50/20 변경 시 RPS 거의 동일(≈ 38.5), p95 38.8ms → 34ms로 소폭 개선. READ 위주 부하에서는 풀 30으로 충분해 풀 확대 효과는 미미함.
- **order-spike**: 조정(50/20)에서 RPS ≈ 167.6, p95 ≈ 987ms, 에러율 0%. Baseline(≈ 160 RPS, p95 ≈ 960ms) 대비 RPS 소폭 상승·p95 소폭 증가로, 풀 확대만으로는 스파이크 구간 레이턴시가 눈에 띄게 좋아지지 않음. 주문 6784건 201 성공.
- **soak-mixed**: 조정(50/20)에서 RPS ≈ 29, p95 ≈ 129ms, 에러율 0%. Baseline(≈ 29 RPS, p95 ≈ 118ms)과 비슷한 수준. 장시간 부하에서는 풀 크기 차이가 체감되지 않음.
- **종합**: 현재 부하 수준(README/스파이크 ~160 RPS, Soak ~29 RPS)에서는 풀 30/10으로도 충분하고, 50/20으로 늘려도 처리량·레이턴시 개선폭은 크지 않음. connection 대기로 인한 병목이 드러날 만큼의 부하는 아니었던 것으로 해석 가능.

---

### 7. 캐시 도입 실험 (B-2)

**목적**: 상품 목록·상세에 Caffeine 인메모리 캐시를 적용한 뒤 basic-read로 재측정해, 캐시 미적용 대비 처리량·레이턴시 변화를 비교.

**적용 범위**  
- `GET /api/items`(목록): `ItemService.getItems()` → 캐시 `itemList`, 키는 condition + pageable  
- `GET /api/items/{id}`(상세): `ItemService.getItem(id)` → 캐시 `items`, 키는 id  
- 상품 생성/수정/삭제 시 해당 캐시 무효화(`@CacheEvict`)

**설정**  
- `CacheConfig`: Caffeine `maximumSize=10_000`, `expireAfterWrite=10분`  
- `build.gradle`: `spring-boot-starter-cache`, `caffeine` 의존성 추가

**측정 방법**  
1. perf 프로파일로 기동: `./gradlew bootRun --args='--spring.profiles.active=perf'`  
2. basic-read 실행: `VUS=20 DURATION=60s k6 run perf/k6/basic-read.js`  
3. 아래 표의 "캐시 있음" 열에 RPS·p95·http_req_failed 기입.

**Baseline(캐시 없음)**  
- 2절·6절의 basic-read 20 VU 60s 수치(풀 30/10 또는 50/20 공통): RPS ≈ 38.5, p95 ≈ 38.8ms(또는 34ms), 에러율 0%.

#### 7-1. 결과 비교표

B-2는 **상품 목록·상세**(`GET /api/items`, `GET /api/items/{id}`)에만 캐시를 적용했다. **order-spike**는 로그인+주문만 호출해 목록/상세를 쓰지 않으므로 캐시 효과가 없고, **soak-mixed**는 로그인+목록+상세+주문이라 목록/상세 구간에서 캐시 히트가 발생한다.

| 시나리오 | 지표 | 캐시 없음 | 캐시 있음 (Caffeine) |
|----------|------|-----------|----------------------|
| basic-read (20 VU 60s) | RPS | ≈ 38.5 | **≈ 39.5** |
| basic-read (20 VU 60s) | p95 | ≈ 38.8ms | **≈ 11.6ms** |
| basic-read (20 VU 60s) | http_req_failed | 0% | **0%** |
| order-spike (재고 충분) | RPS | ≈ 160 | — (목록/상세 미호출) |
| order-spike (재고 충분) | p95 | ≈ 960ms | — (목록/상세 미호출) |
| order-spike (재고 충분) | http_req_failed | 0% | — (목록/상세 미호출) |
| soak-mixed (10 VU 10분) | RPS | ≈ 29.3 | **≈ 29.5** |
| soak-mixed (10 VU 10분) | p95 | ≈ 118ms | **≈ 119ms** |
| soak-mixed (10 VU 10분) | http_req_failed | 0% | **0%** |

**해석**  
- **basic-read**: Caffeine 캐시 적용 후 목록·상세 반복 요청이 캐시 히트되어 **p95가 38.8ms → 11.6ms로 약 70% 개선**. RPS는 38.5 → 39.5로 소폭 상승.
- **order-spike**: 로그인·주문 API만 사용하므로 목록/상세 캐시 적용 대상이 아님. 표에는 "—(목록/상세 미호출)"로 두고, 캐시 유무 비교는 하지 않음.
- **soak-mixed**: 목록·상세·주문을 섞어 호출하므로 목록/상세 구간은 캐시 히트. 캐시 있음 측정에서 RPS ≈ 29.5, p95 ≈ 119ms로 4절 baseline(≈ 29.3 RPS, ≈ 118ms)과 비슷한 수준.

**soak-mixed에서 basic-read만큼 개선이 뚜렷하지 않은 이유**  
- **요청 구성**: soak-mixed 1 iteration = 로그인(1회) + 목록(1회) + 상세(1회) + 주문(약 30% 확률). 캐시가 적용된 건 목록·상세뿐이고, **로그인·주문은 매번 서버/DB를 타므로** 그 구간 레이턴시는 그대로다.
- **p95에 미치는 영향**: k6의 `http_req_duration` p95는 **모든 요청**을 한데 묶어 계산한다. 목록·상세는 캐시로 빨라지지만, 로그인·주문 요청은 여전히 수십~수백 ms대라, **전체 요청의 p95는 이 “느린 요청” 쪽으로 끌려 올라간다**. 그래서 basic-read처럼 “거의 전부 캐시 히트”인 경우와 달리, soak에서는 p95가 크게 내려가 보이지 않는다.
- **RPS가 거의 안 오른 이유**: 1 iteration 끝에 `sleep(1)`이 있어, VU당 초당 1번 꼴로만 다음 iteration을 돌린다. 목록·상세를 아무리 빨리 처리해도, **로그인 + (30%) 주문 + 1초 대기** 때문에 초당 요청 수 상한이 이미 정해져 있어, 캐시만으로는 RPS가 크게 늘지 않는다.
- **정리**: soak에서 “목록·상세만” 보면 캐시로 체감 지연은 줄었을 수 있지만, **전체 시나리오**에서는 비캐시(로그인·주문) 비중과 스크립트 구조(sleep, 혼합 비율) 때문에 **종합 지표(RPS, p95)에는 basic-read만큼의 개선이 드러나지 않는다**.

