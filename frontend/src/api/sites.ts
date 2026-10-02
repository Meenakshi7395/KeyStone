
import { apiClient } from "./client";
import type { Page, Site, SiteRequest } from "../types";

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


// PUT /api/customers/{customerId}/sites/{siteId}
export function updateSite(customerId: number, siteId: number, payload: SiteRequest) {
  return apiClient
    .put<Site>(`/api/customers/${customerId}/sites/${siteId}`, payload)
    .then((response) => response.data);
}

// GET /api/sites?search=&customerId=&page=&size= (staff)
export function searchSites(params: { search?: string; customerId?: number; page?: number; size?: number } = {}) {
  return apiClient.get<Page<Site>>("/api/sites", { params }).then((response) => response.data);
}
