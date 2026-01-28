/**
 * 상품 분산 주문 시나리오 (order-spike-distributed)
 *
 * order-spike.js와 달리 "동일 아이템 몰림"이 아니라,
 * 목록에서 여러 아이템 ID를 골라 분산 주문한다.
 * - 동일 상품 집중 vs 분산 시 처리량·에러율·락 경합 차이 비교용.
 *
 * 환경 변수:
 *   TEST_EMAIL, TEST_PASSWORD — 로그인 계정
 *   BASE_URL — 기본 http://localhost:8080
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";
import { BASE_URL, defaultHttpParams } from "./lib/config.js";
import { login, authHeaders } from "./lib/auth.js";

const TEST_EMAIL = __ENV.TEST_EMAIL || "user1@example.com";
const TEST_PASSWORD = __ENV.TEST_PASSWORD || "password";
const QUANTITY = Number(__ENV.QUANTITY || 1);

// 부하 패턴: order-spike와 동일하게 0→200 VU 10s, 200 유지 60s (환경변수로 오버라이드 가능)
const RAMP_UP = __ENV.RAMP_UP || "10s";
const RAMP_TARGET = Number(__ENV.RAMP_TARGET || 200);
const HOLD_DURATION = __ENV.HOLD_DURATION || "60s";
const RAMP_DOWN = __ENV.RAMP_DOWN || "10s";

const orders_total = new Counter("orders_total");
const orders_success_201 = new Counter("orders_success_201");
const orders_400_insufficient_stock = new Counter("orders_400_insufficient_stock");
const orders_400_timedeal_not_opened = new Counter(
  "orders_400_timedeal_not_opened"
);
const orders_4xx_other = new Counter("orders_4xx_other");
const orders_5xx = new Counter("orders_5xx");

export const options = {
  scenarios: {
    orderSpikeDistributed: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: RAMP_UP, target: RAMP_TARGET },
        { duration: HOLD_DURATION, target: RAMP_TARGET },
        { duration: RAMP_DOWN, target: 0 },
      ],
      gracefulStop: "30s",
      exec: "orderScenario",
    },
  },
};

function pickItemIdFromList(token) {
  const listRes = http.get(`${BASE_URL}/api/items?size=50`, defaultHttpParams);
  if (listRes.status !== 200) return null;
  let ids = [];
  try {
    const body = JSON.parse(listRes.body);
    if (Array.isArray(body.content) && body.content.length > 0) {
      ids = body.content.map((i) => i.id).filter((id) => id != null);
    }
  } catch (e) {
    return null;
  }
  if (ids.length === 0) return null;
  return ids[Math.floor(Math.random() * ids.length)];
}

export function orderScenario() {
  const token = login(TEST_EMAIL, TEST_PASSWORD);
  const itemId = pickItemIdFromList(token);
  if (itemId == null) {
    sleep(1);
    return;
  }

  const payload = JSON.stringify({
    itemId,
    quantity: QUANTITY,
  });

  const res = http.post(
    `${BASE_URL}/api/orders`,
    payload,
    authHeaders(token)
  );

  check(res, {
    "order status is 201": (r) => r.status === 201,
    "order business error or success": (r) =>
      [200, 201, 400, 404].includes(r.status),
  });

  orders_total.add(1);
  const status = res.status;
  if (status === 201) {
    orders_success_201.add(1);
  } else if (status >= 500) {
    orders_5xx.add(1);
  } else if (status >= 400 && status < 500) {
    let message = "";
    try {
      const body = JSON.parse(res.body);
      message = body.message || "";
    } catch (e) {}

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
