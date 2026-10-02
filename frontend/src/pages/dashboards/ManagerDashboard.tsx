import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as reportsApi from "../../api/reports";
import * as usersApi from "../../api/users";
import * as sitesApi from "../../api/sites";
import * as workOrdersApi from "../../api/workOrders";
import { apiErrorMessage } from "../../api/client";
import StatCard from "../../components/StatCard";
import SlaBadge from "../../components/SlaBadge";
import { PriorityTag, StatusPill } from "../../components/StatusPill";
import { useAuth } from "../../context/AuthContext";
import type { ReportSummary, Site, User, WorkOrder, WorkOrderFilters } from "../../types";
import { STATUS_LABEL, STATUS_ORDER, woCode } from "../../lib/workOrders";

type Period = "7" | "30" | "90" | "all";
type Breakdown = "technician" | "site";

function isoDaysAgo(days: number) {
  const d = new Date(Date.now() - days * 86_400_000);
  return d.toISOString().slice(0, 10);
}

/**
 * Manager dashboard (F8). Figures come from GET /api/reports/summary and the
 * lists from GET /api/work-orders — both with the same filters, so every
 * number on the page agrees with the filter bar.
 */
export default function ManagerDashboard() {
  const { user } = useAuth();
  const [summary, setSummary] = useState<ReportSummary | null>(null);
  const [overdue, setOverdue] = useState<WorkOrder[]>([]);
  const [recent, setRecent] = useState<WorkOrder[]>([]);
  const [techs, setTechs] = useState<User[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [slaMsg, setSlaMsg] = useState<string | null>(null);

  const [period, setPeriod] = useState<Period>("30");
  const [siteId, setSiteId] = useState("");
  const [techId, setTechId] = useState("");
  const [breakdown, setBreakdown] = useState<Breakdown>("technician");

  useEffect(() => {
    usersApi.listTechnicians().then(setTechs).catch(() => setTechs([]));
    sitesApi
      .searchSites({ size: 200 })
      .then((p) => setSites(p.content))
      .catch(() => setSites([]));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    const from = period === "all" ? undefined : isoDaysAgo(Number(period));
    const common = {
      from,
      siteId: siteId ? Number(siteId) : undefined,
      technicianId: techId ? Number(techId) : undefined,
    };
    const list: WorkOrderFilters = { ...common };
    try {
      const [s, o, r] = await Promise.all([
        reportsApi.getSummary(common),
        workOrdersApi.searchWorkOrders({ ...list, overdue: true, size: 6, sort: "slaDueDate,asc" }),
        workOrdersApi.searchWorkOrders({ ...list, size: 5, sort: "createdAt,desc" }),
      ]);
      setSummary(s);
      setOverdue(o.content);
      setRecent(r.content);
    } catch (err) {
      setError(apiErrorMessage(err, "Could not load dashboard data."));
    } finally {
      setLoading(false);
    }
  }, [period, siteId, techId]);

  useEffect(() => {
    load();
  }, [load]);

  async function runSlaCheck() {
    setSlaMsg(null);
    try {
      const res = await reportsApi.runSlaCheck();
      setSlaMsg(`SLA check done: ${res.newlyBreached} newly breached, ${res.newlyAtRisk} newly at risk.`);
      load();
    } catch (err) {
      setSlaMsg(apiErrorMessage(err, "SLA check failed."));
    }
  }

  const s = summary;
  const dash = (v: number | null | undefined) => (loading && !s ? "—" : v ?? "—");
  const byStatus = STATUS_ORDER.map((st) => ({ status: st, count: s?.countsByStatus?.[st] ?? 0 }));
  const maxCount = Math.max(1, ...byStatus.map((b) => b.count));
  const rows = (breakdown === "technician" ? s?.byTechnician : s?.bySite) ?? [];
  const compliance = s?.slaCompliancePercent ?? null;
  const filtersOn = period !== "all" || !!siteId || !!techId;

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Operations overview</h1>
        <p className="page-header__subtitle">
          Good to see you, {user?.name.split(" ")[0]}. Status mix, SLA performance and who's carrying the load —
          every number follows the filters below.
        </p>
      </div>

      <div className="filter-bar">
        <div className="segmented" role="group" aria-label="Period">
          {(["7", "30", "90", "all"] as Period[]).map((p) => (
            <button key={p} className={period === p ? "is-on" : ""} onClick={() => setPeriod(p)}>
              {p === "all" ? "All time" : `${p} days`}
            </button>
          ))}
        </div>
        <select value={siteId} onChange={(e) => setSiteId(e.target.value)} aria-label="Site">
          <option value="">All sites</option>
          {sites.map((st) => (
            <option key={st.id} value={st.id}>
              {st.name}
              {st.customerName ? ` — ${st.customerName}` : ""}
            </option>
          ))}
        </select>
        <select value={techId} onChange={(e) => setTechId(e.target.value)} aria-label="Technician">
          <option value="">All technicians</option>
          {techs.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
        {filtersOn && (
          <button
            className="btn btn--ghost btn--small"
            onClick={() => {
              setPeriod("all");
              setSiteId("");
              setTechId("");
            }}
          >
            Clear filters
          </button>
        )}
        <button className="btn btn--ghost btn--small filter-bar__count" onClick={runSlaCheck} title="Run the SLA monitor now">
          ◷ Run SLA check
        </button>
      </div>

      {error && <div className="form-error">{error}</div>}
      {slaMsg && <div className="success-note">{slaMsg}</div>}

      <div className="stat-grid">
        <StatCard label="Work orders" value={dash(s?.totalWorkOrders)} hint="Raised in period" />
        <StatCard label="Active" value={dash(s?.activeWorkOrders)} hint="Not yet finished" />
        <StatCard label="Unassigned" value={dash(s?.openWorkOrders)} />
        <StatCard
          label="Avg. resolution"
          value={s?.averageResolutionHours != null ? `${s.averageResolutionHours}h` : dash(null)}
          hint="Raised → completed"
        />
        <StatCard label="At risk" value={dash(s?.atRiskWorkOrders)} hint="Close to deadline" />
        <StatCard label="Overdue" value={dash(s?.overdueWorkOrders)} hint="Past SLA" />
      </div>

      <div className="split split--wide-left">
        <section className="panel">
          <h2>Work orders by status</h2>
          {s && s.totalWorkOrders === 0 ? (
            <p className="muted-note">No work orders match these filters.</p>
          ) : (
            <div className="bars bars--7" role="img" aria-label="Bar chart of work orders by status">
              {byStatus.map((b) => (
                <div key={b.status} className="bars__col" title={`${STATUS_LABEL[b.status]}: ${b.count}`}>
                  <span className="bars__value">{s ? b.count : ""}</span>
                  <div className="bars__track">
                    <div className="bars__bar" style={{ height: `${(b.count / maxCount) * 100}%` }} />
                  </div>
                  <span className="bars__label">{STATUS_LABEL[b.status]}</span>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="panel">
          <h2>SLA compliance</h2>
          <div className="hero-number">
            <span className="hero-number__value">{!s ? "—" : compliance === null ? "n/a" : `${compliance}%`}</span>
            <span className="hero-number__label">
              {s && s.slaMet + s.slaBreached > 0
                ? `${s.slaMet} met · ${s.slaBreached} breached`
                : "No jobs have reached their SLA yet"}
            </span>
          </div>
          {compliance !== null && (
            <div className="meter meter--lg" title={`${compliance}% met`}>
              <span
                className={`meter__fill meter__fill--${compliance >= 90 ? "ok" : compliance >= 70 ? "low" : "out"}`}
                style={{ width: `${compliance}%` }}
              />
            </div>
          )}
          <p className="muted-note">
            Finished jobs are judged on their completion time; open jobs count once they're already past due.
          </p>
        </section>
      </div>

      <div className="split">
        <section className="panel">
          <div className="panel__toolbar">
            <h2>Overdue now</h2>
            <Link className="btn btn--ghost btn--small" to="/board">
              Open board →
            </Link>
          </div>
          {overdue.length === 0 ? (
            <p className="muted-note">{loading ? "Loading…" : "Nothing overdue. 🎯"}</p>
          ) : (
            <ul className="row-list">
              {overdue.map((o) => (
                <li key={o.id}>
                  <Link to={`/work-orders/${o.id}`} className="row-list__main">
                    <span className="mono">{woCode(o)}</span> {o.title}
                    <span className="cell-sub">
                      {o.siteName} · {o.technicianName ?? "Unassigned"}
                    </span>
                  </Link>
                  <PriorityTag priority={o.priority} />
                  <SlaBadge workOrder={o} />
                </li>
              ))}
              {s && s.overdueWorkOrders > overdue.length && (
                <li>
                  <Link className="muted-note" to="/work-orders">
                    +{s.overdueWorkOrders - overdue.length} more in the list →
                  </Link>
                </li>
              )}
            </ul>
          )}
        </section>

        <section className="panel">
          <div className="panel__toolbar">
            <h2>Workload by {breakdown}</h2>
            <div className="segmented">
              <button className={breakdown === "technician" ? "is-on" : ""} onClick={() => setBreakdown("technician")}>
                Technician
              </button>
              <button className={breakdown === "site" ? "is-on" : ""} onClick={() => setBreakdown("site")}>
                Site
              </button>
            </div>
          </div>
          <div className="data-table">
            <table>
              <thead>
                <tr>
                  <th>{breakdown === "technician" ? "Technician" : "Site"}</th>
                  <th style={{ textAlign: "right" }}>Active</th>
                  <th style={{ textAlign: "right" }}>Done</th>
                  <th style={{ textAlign: "right" }}>Overdue</th>
                  <th style={{ textAlign: "right" }}>SLA met</th>
                </tr>
              </thead>
              <tbody>
                {rows.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="data-table__status">{loading ? "Loading…" : "No data."}</td>
                  </tr>
                ) : (
                  rows.map((r) => {
                    const judged = r.slaMet + r.slaBreached;
                    return (
                      <tr key={`${r.id}-${r.name}`}>
                        <td>
                          <strong>{r.name}</strong>
                        </td>
                        <td style={{ textAlign: "right" }}>{r.active}</td>
                        <td style={{ textAlign: "right" }}>{r.completed}</td>
                        <td style={{ textAlign: "right" }}>
                          {r.overdue ? <span className="stock stock--out">{r.overdue}</span> : 0}
                        </td>
                        <td style={{ textAlign: "right" }}>
                          {judged ? `${Math.round((r.slaMet / judged) * 100)}%` : "—"}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <section className="panel">
        <h2>Recently raised</h2>
        <ul className="row-list">
          {recent.map((o) => (
            <li key={o.id}>
              <Link to={`/work-orders/${o.id}`} className="row-list__main">
                <span className="mono">{woCode(o)}</span> {o.title}
                <span className="cell-sub">
                  {o.customerName} · {o.siteName}
                </span>
              </Link>
              <StatusPill status={o.status} />
            </li>
          ))}
          {!loading && recent.length === 0 && <li className="muted-note">Nothing in this period.</li>}
        </ul>
      </section>

      <div className="quick-links">
        <Link to="/users" className="quick-link">
          <strong>{s ? s.totalTechnicians : "—"}</strong> technicians
          <span>Manage staff and customer accounts</span>
        </Link>
        <Link to="/customers" className="quick-link">
          <strong>{s ? s.totalCustomers : "—"}</strong> customers
          <span>Organisations Meridian serves</span>
        </Link>
        <Link to="/parts" className="quick-link">
          <strong>Parts</strong> inventory
          <span>Stock levels, costs and restocking</span>
        </Link>
        <Link to="/sites" className="quick-link">
          <strong>Sites</strong>
          <span>Customer buildings</span>
        </Link>
      </div>
    </div>
  );
}
