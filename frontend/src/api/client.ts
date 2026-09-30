import axios from "axios";
import type { ApiError } from "../types";

export const apiClient = axios.create({
  baseURL: "http://localhost:8080",
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
  (response) => response,

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

