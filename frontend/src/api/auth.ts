import { apiClient } from "./client";
import type { AuthResponse, CreateUserRequest, LoginRequest, User } from "../types";

export function login(payload: LoginRequest) {
  return apiClient.post<AuthResponse>("/api/users/login", payload).then((r) => r.data);
}

// Self-registration — POST /api/users is permitAll on the backend.
export function register(payload: CreateUserRequest) {
  return apiClient.post<User>("/api/users", payload).then((r) => r.data);
}
