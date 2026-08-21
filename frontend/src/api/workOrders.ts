import { apiClient } from "./client";
import type {
  Page,
  WorkOrder,
  WorkOrderRequest,
} from "../types";

export function listWorkOrders(
  page = 0,
  size = 10
) {
  return apiClient
    .get<Page<WorkOrder>>("/api/work-orders", {
      params: { page, size },
    })
    .then((r) => r.data);
}

export function getWorkOrder(id: number) {
  return apiClient
    .get<WorkOrder>(`/api/work-orders/by-id/${id}`)
    .then((r) => r.data);
}

export function createWorkOrder(
  payload: WorkOrderRequest
) {
  return apiClient
    .post<WorkOrder>("/api/work-orders", payload)
    .then((r) => r.data);
}