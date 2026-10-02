import type { Role, User, WorkOrder, WorkOrderStatus } from "../types";

/** Lifecycle order used for charts, filters and steppers. */
export const STATUS_ORDER: WorkOrderStatus[] = [
  "OPEN",
  "ASSIGNED",
  "IN_PROGRESS",
  "ON_HOLD",
  "COMPLETED",
  "CLOSED",
  "CANCELLED",
];

/** Columns on the Kanban board (cancelled jobs are left off). */
export const BOARD_COLUMNS: WorkOrderStatus[] = ["OPEN", "ASSIGNED", "IN_PROGRESS", "ON_HOLD", "COMPLETED", "CLOSED"];

export const STATUS_LABEL: Record<WorkOrderStatus, string> = {
  OPEN: "Open",
  ASSIGNED: "Assigned",
  IN_PROGRESS: "In progress",
  ON_HOLD: "On hold",
  COMPLETED: "Completed",
  CLOSED: "Closed",
  CANCELLED: "Cancelled",
};

/** Human-readable code from the API (falls back to the id for very old rows). */
export function woCode(wo: Pick<WorkOrder, "id"> & { code?: string | null }) {
  return wo.code || `WO-${String(wo.id).padStart(4, "0")}`;
}

/** Finished = no longer active work (completed, closed or cancelled). */
export function isFinished(wo: Pick<WorkOrder, "status">) {
  return wo.status === "COMPLETED" || wo.status === "CLOSED" || wo.status === "CANCELLED";
}

export function isOverdue(wo: WorkOrder, now = Date.now()) {
  if (wo.slaState) return wo.slaState === "BREACHED" && !isFinished(wo);
  return !!wo.slaDueDate && !isFinished(wo) && new Date(wo.slaDueDate).getTime() < now;
}

export function isAtRisk(wo: WorkOrder) {
  return wo.slaState === "AT_RISK";
}

function humanDuration(ms: number) {
  const mins = Math.round(Math.abs(ms) / 60_000);
  if (mins < 60) return `${mins}m`;
  const hours = Math.round(mins / 60);
  if (hours < 48) return `${hours}h`;
  return `${Math.round(hours / 24)}d`;
}

export type SlaBadgeState = "none" | "done" | "ok" | "risk" | "breach" | "missed";

/** Badge state + label. Uses the server's SLA state, adds a live countdown. */
export function slaInfo(wo: WorkOrder, now = Date.now()): { state: SlaBadgeState; label: string } {
  if (wo.status === "CANCELLED") return { state: "none", label: "Cancelled" };
  if (!wo.slaDueDate) return { state: "none", label: "No SLA" };
  const diff = new Date(wo.slaDueDate).getTime() - now;
  if (isFinished(wo)) {
    return wo.slaState === "BREACHED" ? { state: "missed", label: "SLA missed" } : { state: "done", label: "SLA met" };
  }
  if (diff < 0 || wo.slaState === "BREACHED") return { state: "breach", label: `Overdue ${humanDuration(diff)}` };
  if (wo.slaState === "AT_RISK") return { state: "risk", label: `Due in ${humanDuration(diff)}` };
  return { state: "ok", label: `Due in ${humanDuration(diff)}` };
}

export interface Transition {
  to: WorkOrderStatus;
  label: string;
  tone: "primary" | "secondary" | "ghost" | "danger";
  needsNote?: boolean;
}

/**
 * Status changes the current user may request. Mirrors the backend's
 * WorkOrderLifecycle + role rules (and GET /transitions), so the UI only
 * offers moves the server accepts. The server still enforces all of it.
 */
export function allowedTransitions(wo: WorkOrder, user: User | null): Transition[] {
  if (!user) return [];
  const role: Role = user.role;
  const isAssignedTech = role === "TECHNICIAN" && wo.technicianId === user.id;
  const staff = role === "MANAGER" || role === "DISPATCHER";
  if (!staff && !isAssignedTech) return [];

  const cancel: Transition = { to: "CANCELLED", label: "Cancel job", tone: "danger", needsNote: true };

  switch (wo.status) {
    case "OPEN":
      return staff ? [cancel] : [];
    case "ASSIGNED":
      return [
        { to: "IN_PROGRESS", label: "Start job", tone: "primary" },
        { to: "ON_HOLD", label: "Put on hold", tone: "ghost", needsNote: true },
        ...(staff ? [cancel] : []),
      ];
    case "IN_PROGRESS":
      return [
        { to: "COMPLETED", label: "Mark complete", tone: "primary" },
        { to: "ON_HOLD", label: "Put on hold", tone: "ghost", needsNote: true },
      ];
    case "ON_HOLD":
      return [{ to: "IN_PROGRESS", label: "Resume", tone: "primary" }];
    case "COMPLETED":
      return [
        ...(role === "MANAGER" ? [{ to: "CLOSED" as WorkOrderStatus, label: "Close job", tone: "primary" as const }] : []),
        ...(staff ? [{ to: "IN_PROGRESS" as WorkOrderStatus, label: "Reopen", tone: "ghost" as const, needsNote: true }] : []),
      ];
    default:
      return [];
  }
}

export function formatDateTime(value?: string | null) {
  if (!value) return "—";
  return new Date(value).toLocaleString(undefined, {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatMinutes(total: number) {
  if (!total) return "0m";
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h ? (m ? `${h}h ${m}m` : `${h}h`) : `${m}m`;
}

const inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });

export function formatMoney(value?: number | null) {
  return value === null || value === undefined ? "—" : inr.format(value);
}
