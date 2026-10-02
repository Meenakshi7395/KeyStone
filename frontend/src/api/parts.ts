import { apiClient } from "./client";
import type { Part, PartRequest } from "../types";

// GET /api/parts
export function listParts() {
  return apiClient.get<Part[]>("/api/parts").then((r) => r.data);
}

// POST /api/parts (manager)
export function createPart(payload: PartRequest) {
  return apiClient.post<Part>("/api/parts", payload).then((r) => r.data);
}

// PUT /api/parts/{id} (manager)
export function updatePart(id: number, payload: PartRequest) {
  return apiClient.put<Part>(`/api/parts/${id}`, payload).then((r) => r.data);
}

// POST /api/parts/{id}/restock (manager)
export function restockPart(id: number, quantity: number) {
  return apiClient.post<Part>(`/api/parts/${id}/restock`, { quantity }).then((r) => r.data);
}
