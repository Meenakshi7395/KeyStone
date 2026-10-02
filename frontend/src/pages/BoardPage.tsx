import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import * as workOrdersApi from "../api/workOrders";
import { apiErrorMessage } from "../api/client";
import WorkOrderCard from "../components/WorkOrderCard";
import type { WorkOrder, WorkOrderPriority, WorkOrderStatus } from "../types";
import { BOARD_COLUMNS, STATUS_LABEL, isAtRisk, isOverdue, woCode } from "../lib/workOrders";

const PRIORITIES: WorkOrderPriority[] = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
const PRIORITY_RANK: Record<WorkOrderPriority, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
const CLOSED_LIMIT = 8;

/**
 * Work-order board (F4.4): one column per lifecycle state. Cards sort by
 * priority then SLA so the most urgent work sits at the top of each column.
 */
export default function BoardPage() {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [priority, setPriority] = useState<"" | WorkOrderPriority>("");
  const [technician, setTechnician] = useState("");
  const [onlyAttention, setOnlyAttention] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await workOrdersApi.listWorkOrders(0, 500);
      setOrders(page.content);
    } catch (err) {
      setError(apiErrorMessage(err, "Could not load the board."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const technicians = useMemo(
    () => Array.from(new Set(orders.map((o) => o.technicianName).filter(Boolean) as string[])).sort(),
    [orders]
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return orders.filter((o) => {
      if (priority && o.priority !== priority) return false;
      if (technician === "__none" && o.technicianName) return false;
      if (technician && technician !== "__none" && o.technicianName !== technician) return false;
      if (onlyAttention && !isOverdue(o) && !isAtRisk(o)) return false;
      if (!q) return true;
      return [woCode(o), o.title, o.siteName, o.customerName, o.technicianName ?? ""]
        .join(" ")
        .toLowerCase()
        .includes(q);
    });
  }, [orders, search, priority, technician, onlyAttention]);

  const columns = useMemo(() => {
    const map = new Map<WorkOrderStatus, WorkOrder[]>(BOARD_COLUMNS.map((s) => [s, []]));
    for (const o of filtered) map.get(o.status)?.push(o);
    for (const [status, list] of map) {
      if (status === "CLOSED") {
        list.sort((a, b) => (b.updatedAt ?? b.createdAt).localeCompare(a.updatedAt ?? a.createdAt));
      } else {
        list.sort(
          (a, b) =>
            PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority] ||
            (a.slaDueDate ?? "9").localeCompare(b.slaDueDate ?? "9")
        );
      }
    }
    return map;
  }, [filtered]);

  const overdue = orders.filter((o) => isOverdue(o)).length;
  const atRisk = orders.filter((o) => isAtRisk(o)).length;

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Work-order board</h1>
        <p className="page-header__subtitle">
          Every job by lifecycle stage. Most urgent first in each column — open a card to assign,
          change status, or log parts and time.
        </p>
      </div>

      <div className="board-toolbar">
        <input
          className="search-input"
          placeholder="Search code, title, site, customer…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select value={priority} onChange={(e) => setPriority(e.target.value as "" | WorkOrderPriority)}>
          <option value="">All priorities</option>
          {PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p.charAt(0) + p.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
        <select value={technician} onChange={(e) => setTechnician(e.target.value)}>
          <option value="">All technicians</option>
          <option value="__none">Unassigned</option>
          {technicians.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <label className="toggle">
          <input type="checkbox" checked={onlyAttention} onChange={(e) => setOnlyAttention(e.target.checked)} />
          Needs attention
          <span className="toggle__hint">
            {overdue} overdue · {atRisk} at risk
          </span>
        </label>
        <button className="btn btn--ghost" onClick={load} disabled={loading}>
          ↻ Refresh
        </button>
        <Link className="btn btn--primary" to="/work-orders">
          + New work order
        </Link>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="board">
        {BOARD_COLUMNS.map((status) => {
          const list = columns.get(status) ?? [];
          const visible = status === "CLOSED" ? list.slice(0, CLOSED_LIMIT) : list;
          return (
            <section key={status} className={`board__col board__col--${status.toLowerCase()}`}>
              <header className="board__col-head">
                <span>{STATUS_LABEL[status]}</span>
                <span className="board__count">{loading ? "…" : list.length}</span>
              </header>
              <div className="board__cards">
                {loading ? (
                  <div className="wo-card wo-card--skeleton" />
                ) : visible.length === 0 ? (
                  <div className="board__empty">No jobs</div>
                ) : (
                  visible.map((o) => <WorkOrderCard key={o.id} workOrder={o} showStatus={false} compact />)
                )}
                {status === "CLOSED" && list.length > CLOSED_LIMIT && (
                  <Link className="board__more" to="/work-orders">
                    +{list.length - CLOSED_LIMIT} more in the list
                  </Link>
                )}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}
