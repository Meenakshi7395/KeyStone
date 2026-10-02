import { FormEvent, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import * as workOrdersApi from "../api/workOrders";
import * as customersApi from "../api/customers";
import * as sitesApi from "../api/sites";
import { apiErrorMessage } from "../api/client";
import DataTable, { type Column } from "../components/DataTable";
import { PriorityTag, StatusPill } from "../components/StatusPill";
import SlaBadge from "../components/SlaBadge";
import type { Customer, Page, Site, WorkOrder, WorkOrderPriority, WorkOrderStatus } from "../types";
import { STATUS_LABEL, STATUS_ORDER, formatDateTime, woCode } from "../lib/workOrders";

const PAGE_SIZE = 15;
const PRIORITIES: WorkOrderPriority[] = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
const SORTS: { value: string; label: string }[] = [
  { value: "createdAt,desc", label: "Newest first" },
  { value: "slaDueDate,asc", label: "SLA due soonest" },
  { value: "updatedAt,desc", label: "Recently updated" },
];

interface Form {
  title: string;
  description: string;
  customerId: number;
  siteId: number;
  priority: WorkOrderPriority;
  slaDueDate: string; // datetime-local, "" = derive from priority
}

const emptyForm: Form = { title: "", description: "", customerId: 0, siteId: 0, priority: "MEDIUM", slaDueDate: "" };

/** All work orders — filtering, search, sorting and paging happen on the server. */
export default function WorkOrdersPage() {
  const [result, setResult] = useState<Page<WorkOrder> | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState<Form>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [created, setCreated] = useState<WorkOrder | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [showForm, setShowForm] = useState(false);

  const [search, setSearch] = useState("");
  const [query, setQuery] = useState(""); // debounced search
  const [status, setStatus] = useState<"" | WorkOrderStatus | "OVERDUE" | "ACTIVE">("");
  const [priority, setPriority] = useState<"" | WorkOrderPriority>("");
  const [sort, setSort] = useState(SORTS[0].value);
  const [page, setPage] = useState(0);

  useEffect(() => {
    const t = window.setTimeout(() => setQuery(search.trim()), 300);
    return () => window.clearTimeout(t);
  }, [search]);

  useEffect(() => setPage(0), [query, status, priority, sort]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setResult(
        await workOrdersApi.searchWorkOrders({
          q: query || undefined,
          status:
            status === "ACTIVE"
              ? ["OPEN", "ASSIGNED", "IN_PROGRESS", "ON_HOLD"]
              : status && status !== "OVERDUE"
              ? [status]
              : undefined,
          overdue: status === "OVERDUE" ? true : undefined,
          priority: priority || undefined,
          page,
          size: PAGE_SIZE,
          sort,
        })
      );
    } catch (e) {
      setError(apiErrorMessage(e, "Could not load work orders."));
    } finally {
      setLoading(false);
    }
  }, [query, status, priority, sort, page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    customersApi
      .listCustomers({ page: 0, size: 200 })
      .then((p) => setCustomers(p.content))
      .catch(() => setCustomers([]));
  }, []);

  useEffect(() => {
    if (!form.customerId) {
      setSites([]);
      return;
    }
    sitesApi
      .listSites(form.customerId)
      .then(setSites)
      .catch((e) => setFormError(apiErrorMessage(e, "Could not load sites.")));
  }, [form.customerId]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setCreated(null);
    if (!form.title.trim()) return setFormError("Title is required.");
    if (!form.customerId || !form.siteId) return setFormError("Select a customer and a site.");
    setSubmitting(true);
    try {
      const wo = await workOrdersApi.createWorkOrder({
        title: form.title.trim(),
        description: form.description.trim(),
        customerId: form.customerId,
        siteId: form.siteId,
        priority: form.priority,
        ...(form.slaDueDate ? { slaDueDate: new Date(form.slaDueDate).toISOString() } : {}),
      });
      setCreated(wo);
      setForm(emptyForm);
      if (page === 0) load();
      else setPage(0);
    } catch (e) {
      setFormError(apiErrorMessage(e, "Could not create work order."));
    } finally {
      setSubmitting(false);
    }
  }

  const rows = result?.content ?? [];
  const totalPages = Math.max(1, result?.totalPages ?? 1);

  const columns: Column<WorkOrder>[] = [
    { header: "Code", render: (r) => <Link to={`/work-orders/${r.id}`}>{woCode(r)}</Link> },
    {
      header: "Work order",
      render: (r) => (
        <>
          <strong>{r.title}</strong>
          <div className="cell-sub">
            {r.customerName} · {r.siteName}
          </div>
        </>
      ),
    },
    { header: "Priority", render: (r) => <PriorityTag priority={r.priority} /> },
    { header: "Status", render: (r) => <StatusPill status={r.status} /> },
    { header: "SLA", render: (r) => <SlaBadge workOrder={r} /> },
    { header: "Technician", render: (r) => r.technicianName ?? <span className="muted-note">Unassigned</span> },
    { header: "Raised", render: (r) => formatDateTime(r.createdAt) },
    {
      header: "",
      align: "right",
      render: (r) => (
        <Link className="btn btn--small btn--secondary" to={`/work-orders/${r.id}`}>
          Open
        </Link>
      ),
    },
  ];

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Work orders</h1>
        <p className="page-header__subtitle">
          Every job across all customers. Search, filter and sort, or raise a new work order.
        </p>
      </div>

      <section className="panel">
        <div className="panel__toolbar">
          <h2>New work order</h2>
          <button className="btn btn--ghost btn--small" onClick={() => setShowForm((v) => !v)}>
            {showForm ? "Hide form" : "+ Create"}
          </button>
        </div>

        {created && (
          <div className="success-note">
            Created <Link to={`/work-orders/${created.id}`}>{woCode(created)}</Link> — open it to assign a technician.
          </div>
        )}

        {showForm && (
          <form className="form-grid" onSubmit={submit}>
            <label className="field field--wide">
              <span>Title</span>
              <input
                required
                maxLength={200}
                placeholder="e.g. Chiller not cooling — level 3"
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              />
            </label>
            <label className="field field--wide">
              <span>Description</span>
              <textarea
                rows={2}
                maxLength={2000}
                placeholder="What's wrong, access notes, contact on site…"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              />
            </label>
            <label className="field">
              <span>Customer</span>
              <select
                required
                value={form.customerId || ""}
                onChange={(e) => setForm((f) => ({ ...f, customerId: Number(e.target.value), siteId: 0 }))}
              >
                <option value="">Select customer</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Site</span>
              <select
                required
                disabled={!form.customerId}
                value={form.siteId || ""}
                onChange={(e) => setForm((f) => ({ ...f, siteId: Number(e.target.value) }))}
              >
                <option value="">{form.customerId ? "Select site" : "Pick a customer first"}</option>
                {sites.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Priority</span>
              <select value={form.priority} onChange={(e) => setForm((f) => ({ ...f, priority: e.target.value as WorkOrderPriority }))}>
                {PRIORITIES.map((p) => (
                  <option key={p} value={p}>
                    {p.charAt(0) + p.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>SLA due (optional)</span>
              <input type="datetime-local" value={form.slaDueDate} onChange={(e) => setForm((f) => ({ ...f, slaDueDate: e.target.value }))} />
            </label>
            <div className="form-grid__foot">
              <span className="muted-note">Leave SLA empty to set it from priority (4h / 1d / 3d / 7d).</span>
              <button className="btn btn--primary" disabled={submitting}>
                {submitting ? "Creating…" : "Create work order"}
              </button>
            </div>
            {formError && <div className="form-error field--wide">{formError}</div>}
          </form>
        )}
      </section>

      <section className="panel">
        <div className="filter-bar">
          <input
            className="search-input"
            placeholder="Search code, title, customer, site, technician…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select value={status} onChange={(e) => setStatus(e.target.value as typeof status)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active (not finished)</option>
            <option value="OVERDUE">Overdue only</option>
            {STATUS_ORDER.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
          <select value={priority} onChange={(e) => setPriority(e.target.value as "" | WorkOrderPriority)}>
            <option value="">All priorities</option>
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {p.charAt(0) + p.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
          <select value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort">
            {SORTS.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
          <span className="muted-note filter-bar__count">{result ? `${result.totalElements} found` : ""}</span>
        </div>

        <DataTable
          columns={columns}
          rows={rows}
          rowKey={(r) => r.id}
          loading={loading}
          error={error}
          emptyMessage="No work orders match these filters."
        />

        {totalPages > 1 && (
          <div className="pagination">
            <button className="btn btn--ghost btn--small" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              ← Prev
            </button>
            <span>
              Page {page + 1} of {totalPages}
            </span>
            <button className="btn btn--ghost btn--small" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
              Next →
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
