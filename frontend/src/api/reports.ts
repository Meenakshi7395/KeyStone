import { apiClient } from "./client";
import type { ReportSummary } from "../types";

export interface ReportFilters {
  from?: string; // yyyy-mm-dd
  to?: string;
  siteId?: number;
  technicianId?: number;
  customerId?: number;
}

// GET /api/reports/summary (manager)
export function getSummary(filters: ReportFilters = {}) {
  const params: Record<string, string> = {};
  Object.entries(filters).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") params[k] = String(v);
  });
  return apiClient.get<ReportSummary>("/api/reports/summary", { params }).then((r) => r.data);
}

// POST /api/reports/sla-check (manager) — run the SLA monitor now
export function runSlaCheck() {
  return apiClient
    .post<{ newlyAtRisk: number; newlyBreached: number; checkedAt: string }>("/api/reports/sla-check")
    .then((r) => r.data);
}
