import type { WorkOrderPriority, WorkOrderStatus } from "../types";

const STATUS_LABEL: Record<string, string> = {
  OPEN: "Open",
  ASSIGNED: "Assigned",
  IN_PROGRESS: "In progress",
  ON_HOLD: "On hold",
  COMPLETED: "Completed",
  CLOSED: "Closed",
  CANCELLED: "Cancelled",
};

export function StatusPill({ status }: { status: WorkOrderStatus | string }) {
  return (
    <span className={`pill pill--${status.toLowerCase()}`}>
      {STATUS_LABEL[status] ?? status}
    </span>
  );
}

export function PriorityTag({ priority }: { priority: WorkOrderPriority | string }) {
  return (
    <span className={`prio prio--${priority.toLowerCase()}`}>
      <span className="prio__bars">
        <i />
        <i />
        <i />
        <i />
      </span>
      {priority}
    </span>
  );
}
