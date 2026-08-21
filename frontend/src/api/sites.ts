import { apiClient } from "./client";
import type { Site, SiteRequest } from "../types";

export function listSites(customerId:number) {
  return apiClient.get<Site[]>(`/api/customers/${customerId}/sites`).then(r=>r.data);
}
export function createSite(customerId:number,payload:SiteRequest) {
  return apiClient.post<Site>(`/api/customers/${customerId}/sites`,payload).then(r=>r.data);
}
