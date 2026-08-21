import { apiClient } from "./client";
import type { Customer, CustomerRequest, Page } from "../types";

export interface ListCustomersParams {
  page?: number;
  size?: number;
}

export function listCustomers(params: ListCustomersParams = {}) {
  return apiClient
    .get<Page<Customer>>("/api/customers", {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 10,
      },
    })
    .then((r) => r.data);
}

export function createCustomer(payload: CustomerRequest) {
  return apiClient
    .post<Customer>("/api/customers", payload)
    .then((r) => r.data);
}

export function updateCustomer(
  id: number,
  payload: CustomerRequest
) {
  return apiClient
    .put<Customer>(`/api/customers/${id}`, payload)
    .then((r) => r.data);
}