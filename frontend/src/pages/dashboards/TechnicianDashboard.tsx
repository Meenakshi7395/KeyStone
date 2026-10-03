import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import * as workOrdersApi from "../../api/workOrders";
import { apiErrorMessage } from "../../api/client";
import StatCard from "../../components/StatCard";
import EmptyState from "../../components/EmptyState";
import WorkOrderCard from "../../components/WorkOrderCard";
import { useAuth } from "../../context/AuthContext";
import type { WorkOrder, WorkOrderStatus } from "../../types";
import { allowedTransitions, isFinished, isOverdue } from "../../lib/workOrders";

type Tab = "active" | "done";

export default function TechnicianDashboard() {
  const { user } = useAuth();
  const [jobs, setJobs] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [tab, setTab] = useState<Tab>("active");

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const page = await workOrdersApi.listWorkOrdersByTechnician(user.id, 0, 200);
      setJobs(page.content);
    } catch (err) {
      setError(apiErrorMessage(err, "Could not load your jobs."));
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    load();
  }, [load]);

  async function move(job: WorkOrder, to: WorkOrderStatus) {
    setBusyId(job.id);
    setError(null);
    try {
      const updated = await workOrdersApi.updateWorkOrderStatus(job.id, { status: to });
      setJobs((list) => list.map((j) => (j.id === job.id ? updated : j)));
    } catch (err) {
      setError(apiErrorMessage(err, "Could not update the job."));
    } finally {
      setBusyId(null);
    }
  }

  const active = useMemo(
    () =>
      jobs
        .filter((j) => !isFinished(j))
        .sort((a, b) => (a.slaDueDate ?? "9").localeCompare(b.slaDueDate ?? "9")),
    [jobs]
  );
  const done = useMemo(() => jobs.filter(isFinished), [jobs]);
  const count = (s: WorkOrderStatus) => jobs.filter((j) => j.status === s).length;

  if (!user) return null;
  const shown = tab === "active" ? active : done;

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>My jobs</h1>
        <p className="page-header__subtitle">
          Hi {user.name.split(" ")[0]} — jobs assigned to you, soonest SLA first. Start, pause and
          complete them here, and log parts and time on each job.
        </p>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="stat-grid">
        <StatCard label="To start" value={loading ? "—" : count("ASSIGNED")} />
        <StatCard label="In progress" value={loading ? "—" : count("IN_PROGRESS")} />
        <StatCard label="On hold" value={loading ? "—" : count("ON_HOLD")} />
        <StatCard label="Completed" value={loading ? "—" : count("COMPLETED") + count("CLOSED")} />
        <StatCard
          label="Overdue"
          value={loading ? "—" : jobs.filter((j) => isOverdue(j)).length}
          hint="Past SLA, not finished"
        />
      </div>

      <div className="tabs" role="tablist">
        <button
          role="tab"
          aria-selected={tab === "active"}
          className={`tab${tab === "active" ? " tab--active" : ""}`}
          onClick={() => setTab("active")}
        >
          Active <span className="tab__count">{active.length}</span>
        </button>
        <button
          role="tab"
          aria-selected={tab === "done"}
          className={`tab${tab === "done" ? " tab--active" : ""}`}
          onClick={() => setTab("done")}
        >
          Finished <span className="tab__count">{done.length}</span>
        </button>
        <button className="btn btn--ghost btn--small tabs__refresh" onClick={load} disabled={loading}>
          ↻ Refresh
        </button>
      </div>

      {loading ? (
        <div className="card-grid">
          {[0, 1, 2].map((i) => (
            <div key={i} className="wo-card wo-card--skeleton" />
          ))}
        </div>
      ) : shown.length === 0 ? (
        <section className="panel panel--muted">
          <EmptyState
            title={tab === "active" ? "No active jobs" : "Nothing finished yet"}
            description={
              tab === "active"
                ? "When a dispatcher assigns you a work order it will appear here."
                : "Jobs you complete will be listed here."
            }
          />
        </section>
      ) : (
        <div className="card-grid">
          {shown.map((job) => (
            <WorkOrderCard key={job.id} workOrder={job}>
              {allowedTransitions(job, user).map((t) => (
                <button
                  key={t.to}
                  className={`btn btn--small btn--${t.tone}`}
                  disabled={busyId === job.id}
                  onClick={() => move(job, t.to)}
                >
                  {busyId === job.id ? "…" : t.label}
                </button>
              ))}
              <Link className="btn btn--small btn--ghost" to={`/work-orders/${job.id}`}>
                Parts & time →
              </Link>
            </WorkOrderCard>
          ))}
        </div>
      )}
    </div>
  );
}
