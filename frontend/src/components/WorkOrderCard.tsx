import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import type { WorkOrder } from "../types";
import { woCode } from "../lib/workOrders";
import SlaBadge from "./SlaBadge";
import { PriorityTag, StatusPill } from "./StatusPill";

interface Props {
  workOrder: WorkOrder;
  showStatus?: boolean;
  compact?: boolean;
  children?: ReactNode;
}

/** Job card used on the board, technician view and customer portal. */
export default function WorkOrderCard({ workOrder: wo, showStatus = true, compact, children }: Props) {
  return (
    <article className={`wo-card${compact ? " wo-card--compact" : ""}`}>
      <div className="wo-card__head">
        <Link className="wo-card__code" to={`/work-orders/${wo.id}`}>
          {woCode(wo)}
        </Link>
        {showStatus ? <StatusPill status={wo.status} /> : <PriorityTag priority={wo.priority} />}
      </div>

      <Link className="wo-card__title" to={`/work-orders/${wo.id}`}>
        {wo.title}
      </Link>

      <div className="wo-card__meta">
        <span>{wo.siteName}</span>
        {!compact && <span>· {wo.customerName}</span>}
      </div>

      <div className="wo-card__foot">
        {showStatus && <PriorityTag priority={wo.priority} />}
        <SlaBadge workOrder={wo} />
        <span className="wo-card__tech" title="Technician">
          {wo.technicianName ?? "Unassigned"}
        </span>
      </div>

      {children && <div className="wo-card__actions">{children}</div>}
    </article>
  );
}
