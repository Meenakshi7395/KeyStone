import { apiClient } from "./client";
import type { CreateUserRequest, User } from "../types";

// GET /api/users
// Backend returns List<UserResponseDTO>
export function listUsers() {
  return apiClient
    .get<User[]>("/api/users")
    .then((r) => r.data);
}

// GET /api/users/{id}
export function getUser(id: number) {
  return apiClient
    .get<User>(`/api/users/${id}`)
    .then((r) => r.data);
}

// POST /api/users
export function createUser(payload: CreateUserRequest) {
  return apiClient
    .post<User>("/api/users", payload)
    .then((r) => r.data);
}

// PUT /api/users/{id}
export function updateUser(
  id: number,
  payload: CreateUserRequest
) {
  return apiClient
    .put<User>(`/api/users/${id}`, payload)
    .then((r) => r.data);
}

// DELETE /api/users/{id}
export function deleteUser(id: number) {
  return apiClient
    .delete<void>(`/api/users/${id}`)
    .then(() => undefined);
}
// GET /api/users/technicians — dispatcher + manager
export function listTechnicians() {
  return apiClient.get<User[]>("/api/users/technicians").then((r) => r.data);
}
