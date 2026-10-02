import { apiClient } from "./client";
import type {
  AddPartToWorkOrderRequest,
  AssignTechnicianRequest,
  LogTimeRequest,
  Page,
  UpdateWorkOrderStatusRequest,
  WorkOrder,
  WorkOrderFilters,
  WorkOrderHistory,
  WorkOrderPartLine,
  WorkOrderRequest,
  WorkOrderStatus,
  WorkOrderTime,
  WorkOrderTotals,
  WorkOrderUpdateRequest,
} from "../types";

/** Serialise filters; repeated `status` params are sent as status=A&status=B. */
function toParams(f: WorkOrderFilters = {}) {
  const params = new URLSearchParams();
  f.status?.forEach((s) => params.append("status", s));
  const scalar: (keyof WorkOrderFilters)[] = [
    "priority", "technicianId", "unassigned", "siteId", "customerId", "overdue", "q", "from", "to", "page", "size", "sort",
  ];
  for (const key of scalar) {
    const v = f[key];
    if (v !== undefined && v !== null && v !== "") params.append(key, String(v));
  }
  return params;
}

// POST /api/work-orders
export function createWorkOrder(payload: WorkOrderRequest) {
  return apiClient.post<WorkOrder>("/api/work-orders", payload).then((r) => r.data);
}

// GET /api/work-orders — filtered, paginated, role-scoped on the server
export function searchWorkOrders(filters: WorkOrderFilters = {}) {
  return apiClient.get<Page<WorkOrder>>("/api/work-orders", { params: toParams(filters) }).then((r) => r.data);
}

/** Back-compat helper: first N work orders, newest first. */
export function listWorkOrders(page = 0, size = 20) {
  return searchWorkOrders({ page, size });
}

// GET /api/work-orders/{id}
export function getWorkOrder(id: number) {
  return apiClient.get<WorkOrder>(`/api/work-orders/${id}`).then((r) => r.data);
}

// PUT /api/work-orders/{id} — edit while not closed/cancelled
export function updateWorkOrder(id: number, payload: WorkOrderUpdateRequest) {
  return apiClient.put<WorkOrder>(`/api/work-orders/${id}`, payload).then((r) => r.data);
}

// GET /api/work-orders/technician/{technicianId}
export function listWorkOrdersByTechnician(technicianId: number, page = 0, size = 20) {
  return apiClient
    .get<Page<WorkOrder>>(`/api/work-orders/technician/${technicianId}`, { params: { page, size } })
    .then((r) => r.data);
}

// GET /api/work-orders/customer/{customerId}
export function listWorkOrdersByCustomer(customerId: number, page = 0, size = 20) {
  return apiClient
    .get<Page<WorkOrder>>(`/api/work-orders/customer/${customerId}`, { params: { page, size } })
    .then((r) => r.data);
}

// POST /api/work-orders/{id}/assign
export function assignTechnician(workOrderId: number, payload: AssignTechnicianRequest) {
  return apiClient.post<WorkOrder>(`/api/work-orders/${workOrderId}/assign`, payload).then((r) => r.data);
}

// POST /api/work-orders/{id}/status — 409 when the move isn't allowed
export function updateWorkOrderStatus(workOrderId: number, payload: UpdateWorkOrderStatusRequest) {
  return apiClient.post<WorkOrder>(`/api/work-orders/${workOrderId}/status`, payload).then((r) => r.data);
}

// GET /api/work-orders/{id}/transitions — statuses the caller may move to now
export function getAllowedTransitions(workOrderId: number) {
  return apiClient.get<WorkOrderStatus[]>(`/api/work-orders/${workOrderId}/transitions`).then((r) => r.data);
}

// GET /api/work-orders/{id}/history
export function getWorkOrderHistory(workOrderId: number) {
  return apiClient.get<WorkOrderHistory[]>(`/api/work-orders/${workOrderId}/history`).then((r) => r.data);
}

// GET /api/work-orders/{id}/parts
export function getWorkOrderParts(workOrderId: number) {
  return apiClient.get<WorkOrderPartLine[]>(`/api/work-orders/${workOrderId}/parts`).then((r) => r.data);
}

// POST /api/work-orders/{id}/parts — decrements stock in one transaction
export function addPartToWorkOrder(workOrderId: number, payload: AddPartToWorkOrderRequest) {
  return apiClient.post<WorkOrderPartLine>(`/api/work-orders/${workOrderId}/parts`, payload).then((r) => r.data);
}

// GET /api/work-orders/{id}/time
export function getWorkOrderTimeLogs(workOrderId: number) {
  return apiClient.get<WorkOrderTime[]>(`/api/work-orders/${workOrderId}/time`).then((r) => r.data);
}

// POST /api/work-orders/{id}/time
export function logWorkOrderTime(workOrderId: number, payload: LogTimeRequest) {
  return apiClient.post<WorkOrderTime>(`/api/work-orders/${workOrderId}/time`, payload).then((r) => r.data);
}

// GET /api/work-orders/{id}/totals
export function getWorkOrderTotals(workOrderId: number) {
  return apiClient.get<WorkOrderTotals>(`/api/work-orders/${workOrderId}/totals`).then((r) => r.data);
}
