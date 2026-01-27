import http from "k6/http";
import { check } from "k6";
import { BASE_URL, defaultHttpParams } from "./config.js";

export function login(email, password) {
  const payload = JSON.stringify({ email, password });

  const res = http.post(`${BASE_URL}/api/auth/login`, payload, defaultHttpParams);

  check(res, {
    "login status is 200": (r) => r.status === 200,
    "login has token": (r) => {
      try {
        const body = JSON.parse(r.body);
        return !!body.token;
      } catch (e) {
        return false;
      }
    },
  });

  const body = JSON.parse(res.body);
  return body.token;
}

export function authHeaders(token) {
  return {
    headers: {
      ...defaultHttpParams.headers,
      Authorization: `Bearer ${token}`,
    },
  };
}

