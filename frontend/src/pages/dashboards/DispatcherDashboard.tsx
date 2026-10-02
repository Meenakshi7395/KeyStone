import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as customersApi from "../../api/customers";
import * as workOrdersApi from "../../api/workOrders";
import { apiErrorMessage } from "../../api/client";
import StatCard from "../../components/StatCard";
import SlaBadge from "../../components/SlaBadge";
import { PriorityTag } from "../../components/StatusPill";
import { useAuth } from "../../context/AuthContext";
import type { WorkOrder } from "../../types";
import { isAtRisk, isFinished, isOverdue, woCode } from "../../lib/workOrders";

const RANK = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 } as const;

/** Dispatcher home: what needs assigning and what's slipping. */
export default function DispatcherDashboard() {
  const { user } = useAuth();
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [customerCount, setCustomerCount] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([workOrdersApi.listWorkOrders(0, 500), customersApi.listCustomers({ page: 0, size: 1 })])
      .then(([w, c]) => {
        if (cancelled) return;
        setOrders(w.content);
        setCustomerCount(c.totalElements);
      })
      .catch((err) => !cancelled && setError(apiErrorMessage(err, "Could not load dashboard data.")))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, []);

  const unassigned = orders
    .filter((o) => o.status === "OPEN")
    .sort((a, b) => RANK[a.priority] - RANK[b.priority] || (a.slaDueDate ?? "9").localeCompare(b.slaDueDate ?? "9"));
  const slipping = orders
    .filter((o) => isOverdue(o) || isAtRisk(o))
    .sort((a, b) => (a.slaDueDate ?? "").localeCompare(b.slaDueDate ?? ""));
  const dash = (v: number | null) => (loading || v === null ? "—" : v);

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Dispatch desk</h1>
        <p className="page-header__subtitle">
          Morning, {user?.name.split(" ")[0]}. Assign new requests, keep an eye on SLAs, and move work
          across the board.
        </p>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="stat-grid">
        <StatCard label="Needs assignment" value={dash(unassigned.length)} />
        <StatCard label="Assigned" value={dash(orders.filter((o) => o.status === "ASSIGNED").length)} />
        <StatCard label="In progress" value={dash(orders.filter((o) => o.status === "IN_PROGRESS").length)} />
        <StatCard label="On hold" value={dash(orders.filter((o) => o.status === "ON_HOLD").length)} />
        <StatCard label="Overdue" value={dash(orders.filter((o) => isOverdue(o)).length)} />
        <StatCard label="Customers" value={dash(customerCount)} />
      </div>

      <div className="split">
        <section className="panel">
          <div className="panel__toolbar">
            <h2>Needs assignment</h2>
            <Link className="btn btn--ghost btn--small" to="/board">
              Board →
            </Link>
          </div>
          {unassigned.length === 0 ? (
            <p className="muted-note">{loading ? "Loading…" : "Everything is assigned. ✔"}</p>
          ) : (
            <ul className="row-list">
              {unassigned.slice(0, 8).map((o) => (
                <li key={o.id}>
                  <Link to={`/work-orders/${o.id}`} className="row-list__main">
                    <span className="mono">{woCode(o)}</span> {o.title}
                    <span className="cell-sub">
                      {o.customerName} · {o.siteName}
                    </span>
                  </Link>
                  <PriorityTag priority={o.priority} />
                  <Link className="btn btn--secondary btn--small" to={`/work-orders/${o.id}`}>
                    Assign
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="panel">
          <h2>SLA watch</h2>
          {slipping.length === 0 ? (
            <p className="muted-note">{loading ? "Loading…" : "No jobs overdue or at risk."}</p>
          ) : (
            <ul className="row-list">
              {slipping.slice(0, 8).map((o) => (
                <li key={o.id}>
                  <Link to={`/work-orders/${o.id}`} className="row-list__main">
                    <span className="mono">{woCode(o)}</span> {o.title}
                    <span className="cell-sub">{o.technicianName ?? "Unassigned"}</span>
                  </Link>
                  <SlaBadge workOrder={o} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <div className="quick-links">
        <Link to="/work-orders" className="quick-link">
          <strong>+ New</strong> work order
          <span>Raise a job for a customer site</span>
        </Link>
        <Link to="/board" className="quick-link">
          <strong>Board</strong>
          <span>{loading ? "—" : orders.filter((o) => !isFinished(o)).length} active jobs by stage</span>
        </Link>
        <Link to="/customers" className="quick-link">
          <strong>Customers</strong>
          <span>Organisations and contacts</span>
        </Link>
        <Link to="/sites" className="quick-link">
          <strong>Sites</strong>
          <span>Buildings per customer</span>
        </Link>
      </div>
    </div>
  );
}
