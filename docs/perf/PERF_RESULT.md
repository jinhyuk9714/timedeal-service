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

### 4. 현재까지의 결론 및 다음 단계 제안

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

3. **향후 리포트 확장 방향**
   - Soak 테스트(장시간 낮은 RPS 유지) 결과 추가.
   - 튜닝 전/후(예: 인덱스 추가, Hikari 풀 조정, 캐시 도입 등) 수치를 표/그래프로 비교.
   - 비관적 락 전략과 낙관적 락/분산 락과의 비교 실험을 진행한 후, 결과를 이 문서에 추가.

