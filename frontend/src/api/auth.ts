
import { apiClient } from "./client";

import type {
  AuthResponse,
  CreateUserRequest,
  LoginRequest,
  User,
} from "../types";

export function login(payload: LoginRequest) {
  return apiClient
    .post<AuthResponse>("/api/auth/login", payload)
    .then((r) => r.data);
}

export function register(payload: CreateUserRequest) {
  return apiClient
    .post<User>("/api/auth/register", payload)
    .then((r) => r.data);
}