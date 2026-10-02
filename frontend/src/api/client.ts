import axios from "axios";
import type { ApiError } from "../types";

// VITE_API_BASE_URL may be "http://host:8080" or "http://host:8080/api";
// request paths already start with /api, so strip a trailing /api.
const RAW_BASE =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";

export const API_BASE_URL = RAW_BASE.replace(/\/+$/, "").replace(/\/api$/, "");

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// =========================================
// ADD CURRENT JWT TO EVERY REQUEST
// =========================================

apiClient.interceptors.request.use(
  (config) => {
    const token =
      localStorage.getItem("keystone_token");

    if (token) {
      config.headers.Authorization =
        `Bearer ${token}`;
    } else {
      delete config.headers.Authorization;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// =========================================
// HANDLE 401 / 403
// =========================================

apiClient.interceptors.response.use(
  (response) => {
    // Spring Data may serialise pages as { content, page: { size, number, totalElements, totalPages } }.
    // Flatten that into the classic Page<T> shape the UI expects.
    const d = response.data;
    if (d && Array.isArray(d.content) && d.page && typeof d.page === "object" && d.totalElements === undefined) {
      const pg = d.page;
      response.data = {
        content: d.content,
        totalElements: pg.totalElements,
        totalPages: pg.totalPages,
        number: pg.number,
        size: pg.size,
        first: pg.number === 0,
        last: pg.number + 1 >= pg.totalPages,
      };
    }
    return response;
  },

  (error) => {
    if (
      error.response?.status === 401 ||
      error.response?.status === 403
    ) {
      console.error(
        "Authentication/authorization error:",
        error.response.status,
        error.config?.url
      );
    }

    return Promise.reject(error);
  }
);

// =========================================
// API ERROR MESSAGE
// =========================================

export function apiErrorMessage(
  error: unknown,
  fallback = "Something went wrong."
): string {
  if (axios.isAxiosError(error)) {
    const data =
      error.response?.data as ApiError | undefined;

    if (data?.message) {
      return data.message;
    }

    if (data?.error) {
      return data.error;
    }

    if (error.message) {
      return error.message;
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}

