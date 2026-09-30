
import { apiClient } from "./client";
import type { Site, SiteRequest } from "../types";

// GET /api/customers/{customerId}/sites
export function listSites(customerId: number) {
  return apiClient
    .get<Site[]>(`/api/customers/${customerId}/sites`)
    .then((response) => response.data);
}

// POST /api/customers/{customerId}/sites
export function createSite(
  customerId: number,
  payload: SiteRequest
) {
  return apiClient
    .post<Site>(
      `/api/customers/${customerId}/sites`,
      payload
    )
    .then((response) => response.data);
}

