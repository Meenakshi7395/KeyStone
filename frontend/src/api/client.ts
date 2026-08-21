import axios, { AxiosError } from "axios";

export const TOKEN_STORAGE_KEY = "token";

// Docker Compose provides:
// VITE_API_BASE_URL=http://localhost:8080
//
// API calls in the frontend already start with /api,
// for example: /api/users/login
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Automatically attach JWT token to authenticated requests
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

type BackendError = {
  message?: string;
  error?: string;
  errors?: Record<string, string>;
};

// Convert backend errors into a readable message
export function apiErrorMessage(
  error: unknown,
  fallback = "Something went wrong."
): string {
  if (!axios.isAxiosError(error)) {
    return fallback;
  }

  const axiosError = error as AxiosError<unknown>;
  const data = axiosError.response?.data;

  // Backend returned plain text
  if (typeof data === "string") {
    return data.trim() || fallback;
  }

  // Backend returned JSON
  if (data && typeof data === "object") {
    const errorData = data as BackendError;

    if (
      typeof errorData.message === "string" &&
      errorData.message.trim().length > 0
    ) {
      return errorData.message;
    }

    if (errorData.errors) {
      const messages = Object.values(errorData.errors).filter(
        (value): value is string =>
          typeof value === "string" && value.trim().length > 0
      );

      if (messages.length > 0) {
        return messages.join(", ");
      }
    }

    if (
      typeof errorData.error === "string" &&
      errorData.error.trim().length > 0
    ) {
      return errorData.error;
    }
  }

  return fallback;
}