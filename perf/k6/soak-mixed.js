import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, defaultHttpParams } from "./lib/config.js";
import { login, authHeaders } from "./lib/auth.js";

// 환경 변수로 조정 가능
const SOAK_VUS = Number(__ENV.SOAK_VUS || 10);
const SOAK_DURATION = __ENV.SOAK_DURATION || "10m"; // 예: "30m", "1h"

const TEST_EMAIL = __ENV.TEST_EMAIL || "user1@example.com";
const TEST_PASSWORD = __ENV.TEST_PASSWORD || "password";
const ITEM_ID = Number(__ENV.ITEM_ID || 1);

export const options = {
  thresholds: {
    http_req_failed: ["rate<0.01"], // 전체 에러율 1% 미만 목표
  },
  scenarios: {
    soakMixed: {
      executor: "constant-vus",
      vus: SOAK_VUS,
      duration: SOAK_DURATION,
      exec: "soakScenario",
    },
  },
};

export function soakScenario() {
  // 1. 로그인 (세션별 한 번 정도 호출된다고 가정)
  const token = login(TEST_EMAIL, TEST_PASSWORD);

  // 2. 상품 목록 조회
  const listRes = http.get(`${BASE_URL}/api/items`, defaultHttpParams);
  check(listRes, {
    "list status is 200": (r) => r.status === 200,
  });

  // 3. 첫 상품 단건 조회 (있을 때만)
  let firstId = null;
  try {
    const body = JSON.parse(listRes.body);
    if (Array.isArray(body.content) && body.content.length > 0) {
      firstId = body.content[0].id;
    }
  } catch (e) {
    // 무시: 위 check에서 이미 실패로 카운트됨
  }

  if (firstId != null) {
    const detailRes = http.get(
      `${BASE_URL}/api/items/${firstId}`,
      defaultHttpParams
    );
    check(detailRes, {
      "detail status is 200": (r) => r.status === 200,
    });
  }

  // 4. 낮은 비율로 주문 생성 (예: 전체 요청 중 일부만 주문)
  if (Math.random() < 0.3) {
    const orderPayload = JSON.stringify({
      itemId: ITEM_ID,
      quantity: 1,
    });

    const orderRes = http.post(
      `${BASE_URL}/api/orders`,
      orderPayload,
      authHeaders(token)
    );

    check(orderRes, {
      "order status is 201 or 400/404": (r) =>
        [200, 201, 400, 404].includes(r.status),
    });
  }

  sleep(1);
}

