import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";
import { BASE_URL, defaultHttpParams } from "./lib/config.js";
import { login, authHeaders } from "./lib/auth.js";

// 환경 변수로 계정/아이템/수량 조정
const TEST_EMAIL = __ENV.TEST_EMAIL || "user1@example.com";
const TEST_PASSWORD = __ENV.TEST_PASSWORD || "password";
const ITEM_ID = Number(__ENV.ITEM_ID || 1);
const QUANTITY = Number(__ENV.QUANTITY || 1);

// 주문 결과를 집계하기 위한 커스텀 메트릭
const orders_total = new Counter("orders_total");
const orders_success_201 = new Counter("orders_success_201");
const orders_400_insufficient_stock = new Counter(
  "orders_400_insufficient_stock",
);
const orders_400_timedeal_not_opened = new Counter(
  "orders_400_timedeal_not_opened",
);
const orders_4xx_other = new Counter("orders_4xx_other");
const orders_429_too_many_requests = new Counter(
  "orders_429_too_many_requests",
);
const orders_503_service_unavailable = new Counter(
  "orders_503_service_unavailable",
);
const orders_5xx = new Counter("orders_5xx");

export const options = {
  scenarios: {
    orderSpike: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 200 }, // 0 -> 200 VUs
        { duration: "60s", target: 200 }, // 200 VUs 유지
        { duration: "10s", target: 0 }, // 정리
      ],
      gracefulStop: "30s",
      exec: "orderScenario",
    },
  },
};

export function orderScenario() {
  // 1. 로그인
  const token = login(TEST_EMAIL, TEST_PASSWORD);

  // 2. 주문 생성
  const payload = JSON.stringify({
    itemId: ITEM_ID,
    quantity: QUANTITY,
  });

  const res = http.post(`${BASE_URL}/api/orders`, payload, authHeaders(token));

  check(res, {
    "order status is 201": (r) => r.status === 201,
    "order business error or success": (r) =>
      [200, 201, 400, 404].includes(r.status),
  });

  // 주문 결과 집계
  orders_total.add(1);

  const status = res.status;
  if (status === 201) {
    orders_success_201.add(1);
  } else if (status === 429) {
    orders_429_too_many_requests.add(1);
  } else if (status === 503) {
    orders_503_service_unavailable.add(1);
  } else if (status >= 500) {
    orders_5xx.add(1);
  } else if (status >= 400 && status < 500) {
    let message = "";
    try {
      const body = JSON.parse(res.body);
      message = body.message || "";
    } catch (e) {
      // 파싱 실패 시 message는 빈 문자열로 유지
    }

    if (message.includes("재고가 부족합니다")) {
      orders_400_insufficient_stock.add(1);
    } else if (message.includes("타임딜이 아직 시작되지 않았습니다")) {
      orders_400_timedeal_not_opened.add(1);
    } else {
      orders_4xx_other.add(1);
    }
  }

  sleep(1);
}
