export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const defaultHttpParams = {
  headers: {
    "Content-Type": "application/json",
  },
};

export const vus = Number(__ENV.VUS || 1);
export const duration = __ENV.DURATION || "10s";

