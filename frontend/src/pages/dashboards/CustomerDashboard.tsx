import { FormEvent, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as customersApi from "../../api/customers";
import * as sitesApi from "../../api/sites";
import * as workOrdersApi from "../../api/workOrders";
import { apiErrorMessage } from "../../api/client";
import StatCard from "../../components/StatCard";
import EmptyState from "../../components/EmptyState";
import SlaBadge from "../../components/SlaBadge";
import { PriorityTag, StatusPill } from "../../components/StatusPill";
import { useAuth } from "../../context/AuthContext";
import type { Customer, Site, WorkOrder, WorkOrderPriority } from "../../types";
import { formatDateTime, isFinished, woCode } from "../../lib/workOrders";

export default function CustomerDashboard() {
  const { user } = useAuth();
  if (!user) return null;
  return user.customerId ? <CustomerPortal customerId={user.customerId} /> : <NotLinked />;
}

function NotLinked() {
  const { user } = useAuth();
  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Welcome, {user?.name.split(" ")[0]}</h1>
        <p className="page-header__subtitle">Your account isn't linked to an organisation yet.</p>
      </div>
      <section className="panel link-panel">
        <EmptyState
          title="Waiting for Meridian"
          description="Your manager links customer accounts to their organisation and its sites. Once that's done, sign out and sign in again to raise requests."
        />
      </section>
    </div>
  );
}


const emptyRequest = { title: "", description: "", siteId: 0, priority: "MEDIUM" as WorkOrderPriority };

function CustomerPortal({ customerId }: { customerId: number }) {
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [sites, setSites] = useState<Site[]>([]);
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState(emptyRequest);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [justRaised, setJustRaised] = useState<WorkOrder | null>(null);
  const [filter, setFilter] = useState<"open" | "all">("open");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [c, s, w] = await Promise.all([
        customersApi.getCustomer(customerId),
        sitesApi.listSites(customerId),
        workOrdersApi.listWorkOrdersByCustomer(customerId, 0, 200),
      ]);
      setCustomer(c);
      setSites(s);
      setOrders(
        [...w.content].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      );
    } catch (err) {
      setError(apiErrorMessage(err, "Could not load your requests."));
    } finally {
      setLoading(false);
    }
  }, [customerId]);

  useEffect(() => {
    load();
  }, [load]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    if (!form.siteId) return setFormError("Choose the site where the problem is.");
    if (!form.title.trim()) return setFormError("Give the request a short title.");
    setSubmitting(true);
    try {
      const created = await workOrdersApi.createWorkOrder({
        title: form.title.trim(),
        description: form.description.trim(),
        customerId,
        siteId: form.siteId,
        priority: form.priority,
      });
      setJustRaised(created);
      setForm(emptyRequest);
      setOrders((list) => [created, ...list]);
    } catch (err) {
      setFormError(apiErrorMessage(err, "Could not raise the request."));
    } finally {
      setSubmitting(false);
    }
  }

  const openOrders = orders.filter((o) => !isFinished(o));
  const shown = filter === "open" ? openOrders : orders;

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>{customer?.name ?? "Your service requests"}</h1>
        <p className="page-header__subtitle">
          Raise a maintenance request for any of your sites and follow it from dispatch to close-out.
        </p>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="stat-grid">
        <StatCard label="Open requests" value={loading ? "—" : openOrders.filter((o) => o.status === "OPEN").length} hint="Waiting for a technician" />
        <StatCard label="Assigned" value={loading ? "—" : openOrders.filter((o) => o.status === "ASSIGNED").length} />
        <StatCard label="In progress" value={loading ? "—" : openOrders.filter((o) => o.status === "IN_PROGRESS" || o.status === "ON_HOLD").length} />
        <StatCard label="Completed" value={loading ? "—" : orders.filter(isFinished).length} />
        <StatCard label="Your sites" value={loading ? "—" : sites.length} />
      </div>

      <div className="split split--narrow-left">
        <section className="panel">
          <h2>Raise a request</h2>
          {justRaised && (
            <div className="success-note">
              Request <strong>{woCode(justRaised)}</strong> raised. Meridian's dispatch team will
              assign a technician.
            </div>
          )}
          {sites.length === 0 && !loading ? (
            <EmptyState
              title="No sites on file"
              description="Meridian hasn't added any of your buildings yet. Contact your Meridian dispatcher to add a site, then you can raise requests here."
            />
          ) : (
            <form className="stack-form" onSubmit={submit}>
              <label className="field">
                <span>Site</span>
                <select
                  required
                  value={form.siteId || ""}
                  onChange={(e) => setForm((f) => ({ ...f, siteId: Number(e.target.value) }))}
                >
                  <option value="">Select a site…</option>
                  {sites.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} — {s.address}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span>What's wrong?</span>
                <input
                  required
                  maxLength={120}
                  placeholder="e.g. Your problem "
                  value={form.title}
                  onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                />
              </label>
              <label className="field">
                <span>Details (optional)</span>
                <textarea
                  rows={3}
                  maxLength={2000}
                  placeholder="Where exactly, technician should know…"
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                />
              </label>
              <label className="field">
                <span>Urgency</span>
                <select
                  value={form.priority}
                  onChange={(e) => setForm((f) => ({ ...f, priority: e.target.value as WorkOrderPriority }))}
                >
                  <option value="LOW">Low — within a week</option>
                  <option value="MEDIUM">Medium — within 3 days</option>
                  <option value="HIGH">High — within 24 hours</option>
                  <option value="CRITICAL">Critical — within 4 hours</option>
                </select>
              </label>
              {formError && <div className="form-error">{formError}</div>}
              <button className="btn btn--primary" disabled={submitting}>
                {submitting ? "Submitting…" : "Submit request"}
              </button>
            </form>
          )}
        </section>

        <section className="panel">
          <div className="panel__toolbar">
            <h2>Your requests</h2>
            <div className="segmented">
              <button className={filter === "open" ? "is-on" : ""} onClick={() => setFilter("open")}>
                Open ({openOrders.length})
              </button>
              <button className={filter === "all" ? "is-on" : ""} onClick={() => setFilter("all")}>
                All ({orders.length})
              </button>
            </div>
          </div>

          <div className="data-table">
            <table>
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Request</th>
                  <th>Status</th>
                  <th>SLA</th>
                  <th>Technician</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={5} className="data-table__status">Loading…</td>
                  </tr>
                ) : shown.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="data-table__status">
                      {filter === "open" ? "No open requests." : "You haven't raised any requests yet."}
                    </td>
                  </tr>
                ) : (
                  shown.map((o) => (
                    <tr key={o.id}>
                      <td>
                        <Link to={`/work-orders/${o.id}`}>{woCode(o)}</Link>
                      </td>
                      <td>
                        <strong>{o.title}</strong>
                        <div className="cell-sub">
                          {o.siteName} · {formatDateTime(o.createdAt)} · <PriorityTag priority={o.priority} />
                        </div>
                      </td>
                      <td>
                        <StatusPill status={o.status} />
                      </td>
                      <td>
                        <SlaBadge workOrder={o} />
                      </td>
                      <td>{o.technicianName ?? "Not yet assigned"}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}
