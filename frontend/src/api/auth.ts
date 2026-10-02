
import { apiClient } from "./client";

import type {
  AuthResponse,
  LoginRequest,
  User,
} from "../types";

export function login(payload: LoginRequest) {
  return apiClient
    .post<AuthResponse>("/api/auth/login", payload)
    .then((r) => r.data);
}

// GET /api/auth/me — fresh copy of the signed-in user
export function me() {
  return apiClient.get<User>("/api/auth/me").then((r) => r.data);
}
