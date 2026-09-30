
import { apiClient } from "./client";

import type {
  Page,
  WorkOrder,
  WorkOrderRequest,
  AssignTechnicianRequest,
  UpdateWorkOrderStatusRequest,
  WorkOrderHistory,
  AddPartToWorkOrderRequest,
  WorkOrderTime,
  Part,
  LogTimeRequest,
} from "../types";

// =========================================
// CREATE WORK ORDER
// POST /api/work-orders
// =========================================

export function createWorkOrder(
  payload: WorkOrderRequest
) {
  return apiClient
    .post<WorkOrder>(
      "/api/work-orders",
      payload
    )
    .then((r) => r.data);
}


// =========================================
// GET ALL WORK ORDERS
// GET /api/work-orders?page=0&size=10
// Dispatcher / Manager
// =========================================

export function listWorkOrders(
  page = 0,
  size = 10
) {
  return apiClient
    .get<Page<WorkOrder>>(
      "/api/work-orders",
      {
        params: {
          page,
          size,
        },
      }
    )
    .then((r) => r.data);
}


// =========================================
// GET WORK ORDER BY ID
// GET /api/work-orders/{id}
// =========================================

export function getWorkOrder(
  id: number
) {
  return apiClient
    .get<WorkOrder>(
      `/api/work-orders/${id}`
    )
    .then((r) => r.data);
}


// =========================================
// GET WORK ORDERS BY TECHNICIAN
// GET /api/work-orders/technician/{technicianId}
// =========================================

export function listWorkOrdersByTechnician(
  technicianId: number,
  page = 0,
  size = 10
) {
  return apiClient
    .get<Page<WorkOrder>>(
      `/api/work-orders/technician/${technicianId}`,
      {
        params: {
          page,
          size,
        },
      }
    )
    .then((r) => r.data);
}


// =========================================
// GET WORK ORDERS BY CUSTOMER
// GET /api/work-orders/customer/{customerId}
// =========================================

export function listWorkOrdersByCustomer(
  customerId: number,
  page = 0,
  size = 10
) {
  return apiClient
    .get<Page<WorkOrder>>(
      `/api/work-orders/customer/${customerId}`,
      {
        params: {
          page,
          size,
        },
      }
    )
    .then((r) => r.data);
}


// =========================================
// ASSIGN TECHNICIAN
// PUT /api/work-orders/{id}/assign
// =========================================

export function assignTechnician(
  workOrderId: number,
  payload: AssignTechnicianRequest
) {
  return apiClient
    .put<WorkOrder>(
      `/api/work-orders/${workOrderId}/assign`,
      payload
    )
    .then((r) => r.data);
}


// =========================================
// UPDATE WORK ORDER STATUS
// PUT /api/work-orders/{id}/status
// =========================================

export function updateWorkOrderStatus(
  workOrderId: number,
  payload: UpdateWorkOrderStatusRequest
) {
  return apiClient
    .put<WorkOrder>(
      `/api/work-orders/${workOrderId}/status`,
      payload
    )
    .then((r) => r.data);
}


// =========================================
// GET WORK ORDER HISTORY
// GET /api/work-orders/{id}/history
// =========================================

export function getWorkOrderHistory(
  workOrderId: number
) {
  return apiClient
    .get<WorkOrderHistory[]>(
      `/api/work-orders/${workOrderId}/history`
    )
    .then((r) => r.data);
}


// =========================================
// GET PARTS USED
// GET /api/work-orders/{id}/parts
// =========================================

export function getWorkOrderParts(
  workOrderId: number
) {
  return apiClient
    .get<Part[]>(
      `/api/work-orders/${workOrderId}/parts`
    )
    .then((r) => r.data);
}


// =========================================
// ADD / USE PART
// POST /api/work-orders/{id}/parts
// =========================================

export function addPartToWorkOrder(
  workOrderId: number,
  payload: AddPartToWorkOrderRequest
) {
  return apiClient
    .post<void>(
      `/api/work-orders/${workOrderId}/parts`,
      payload
    )
    .then((r) => r.data);
}


// =========================================
// GET TIME LOGS
// GET /api/work-orders/{id}/time
// =========================================

export function getWorkOrderTimeLogs(
  workOrderId: number
) {
  return apiClient
    .get<WorkOrderTime[]>(
      `/api/work-orders/${workOrderId}/time`
    )
    .then((r) => r.data);
}


// =========================================
// LOG TIME
// POST /api/work-orders/{id}/time
// =========================================

export function logWorkOrderTime(
  workOrderId: number,
  payload: LogTimeRequest
) {
  return apiClient
    .post<WorkOrderTime>(
      `/api/work-orders/${workOrderId}/time`,
      payload
    )
    .then((r) => r.data);
}
