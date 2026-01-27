import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    itemsScenario: {
      executor: "constant-vus", // 기본 executor
      vus: 1, // 가상 유저 1명
      duration: "10s", // 10초 실행
      exec: "itemsTest", // 아래에서 export 한 함수 이름
    },
  },
};

export function itemsTest() {
  const res = http.get("http://localhost:8080/api/items");

  console.log(`status = ${res.status}`);

  check(res, {
    "status is 200": (r) => r.status === 200,
  });

  sleep(1); // 1초 쉬고 다시 실행
}

