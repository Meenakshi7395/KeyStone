import type { WorkOrder } from "../types";
import { slaInfo } from "../lib/workOrders";

const ICON: Record<string, string> = {
  breach: "!",
  risk: "◷",
  ok: "✓",
  done: "✓",
  missed: "✕",
  none: "–",
};

/** SLA state with an icon + text so it never relies on colour alone. */
export default function SlaBadge({ workOrder }: { workOrder: WorkOrder }) {
  const { state, label } = slaInfo(workOrder);
  return (
    <span className={`sla sla--${state}`} title={workOrder.slaDueDate ?? "No SLA set"}>
      <span className="sla__icon" aria-hidden="true">
        {ICON[state]}
      </span>
      {label}
    </span>
  );
}
