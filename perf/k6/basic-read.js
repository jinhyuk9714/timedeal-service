import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, defaultHttpParams, vus, duration } from "./lib/config.js";

export const options = {
  scenarios: {
    basicRead: {
      executor: "constant-vus",
      vus,
      duration,
      exec: "basicReadScenario",
    },
  },
};

export function basicReadScenario() {
  // 1. 상품 목록 조회
  const listRes = http.get(`${BASE_URL}/api/items`, defaultHttpParams);
  check(listRes, {
    "list status is 200": (r) => r.status === 200,
  });

  // 2. 첫 번째 아이템 상세 조회 (있을 때만)
  let firstId = null;
  try {
    const body = JSON.parse(listRes.body);
    if (Array.isArray(body.content) && body.content.length > 0) {
      firstId = body.content[0].id;
    }
  } catch (e) {
    // ignore parse error; 체크에서 이미 실패로 잡힘
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

  sleep(1);
}

