import { FormEvent, useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { useAuth } from "../context/AuthContext";
import * as workOrdersApi from "../api/workOrders";
import * as partsApi from "../api/parts";
import * as usersApi from "../api/users";
import * as sitesApi from "../api/sites";
import { apiErrorMessage } from "../api/client";
import { PriorityTag, StatusPill } from "../components/StatusPill";
import SlaBadge from "../components/SlaBadge";
import type {
  Part,
  Site,
  User,
  WorkOrder,
  WorkOrderHistory,
  WorkOrderPartLine,
  WorkOrderPriority,
  WorkOrderStatus,
  WorkOrderTime,
  WorkOrderTotals,
} from "../types";
import {
  STATUS_LABEL,
  type Transition,
  allowedTransitions,
  formatDateTime,
  formatMinutes,
  formatMoney,
  woCode,
} from "../lib/workOrders";

/** Main path of the lifecycle, shown as a stepper. ON_HOLD is a side-state. */
const MAIN_PATH: WorkOrderStatus[] = ["OPEN", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CLOSED"];
const PRIORITIES: WorkOrderPriority[] = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];

function toLocalInput(iso?: string | null) {
  if (!iso) return "";
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function WorkOrderDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const workOrderId = Number(id);

  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [history, setHistory] = useState<WorkOrderHistory[]>([]);
  const [lines, setLines] = useState<WorkOrderPartLine[]>([]);
  const [timeLogs, setTimeLogs] = useState<WorkOrderTime[]>([]);
  const [totals, setTotals] = useState<WorkOrderTotals | null>(null);
  const [catalog, setCatalog] = useState<Part[]>([]);
  const [technicians, setTechnicians] = useState<User[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const [technicianId, setTechnicianId] = useState("");
  const [partId, setPartId] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [hours, setHours] = useState("0");
  const [minutes, setMinutes] = useState("30");
  const [timeNote, setTimeNote] = useState("");

  const [pending, setPending] = useState<Transition | null>(null);
  const [statusNote, setStatusNote] = useState("");

  const [editing, setEditing] = useState(false);
  const [sites, setSites] = useState<Site[]>([]);
  const [edit, setEdit] = useState({ title: "", description: "", siteId: 0, priority: "MEDIUM" as WorkOrderPriority, slaDueDate: "" });

  const role = user?.role;
  const isStaff = role === "MANAGER" || role === "DISPATCHER";
  const isCustomer = role === "CUSTOMER";
  const isAssignedTech = role === "TECHNICIAN" && !!wo && wo.technicianId === user?.id;
  const terminal = wo?.status === "CLOSED" || wo?.status === "CANCELLED";
  const canLogWork = !!wo && !terminal && wo.status !== "OPEN" && (isStaff || isAssignedTech);
  const canAssign = isStaff && !!wo && !["COMPLETED", "CLOSED", "CANCELLED"].includes(wo.status);
  const canEdit = isStaff && !!wo && !terminal;

  const refreshLines = useCallback(async () => {
    const h = await workOrdersApi.getWorkOrderHistory(workOrderId);
    setHistory(h);
    if (isCustomer) return; // parts, time and costs are internal to Meridian
    const [p, t, tot] = await Promise.all([
      workOrdersApi.getWorkOrderParts(workOrderId),
      workOrdersApi.getWorkOrderTimeLogs(workOrderId),
      workOrdersApi.getWorkOrderTotals(workOrderId),
    ]);
    setLines(p);
    setTimeLogs(t);
    setTotals(tot);
  }, [workOrderId, isCustomer]);

  useEffect(() => {
    if (!Number.isInteger(workOrderId)) {
      setError("Invalid work order ID.");
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await workOrdersApi.getWorkOrder(workOrderId);
        if (cancelled) return;
        setWo(data);
        await refreshLines();
      } catch (e) {
        if (!cancelled) setError(apiErrorMessage(e, "Could not load work order."));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [workOrderId, refreshLines]);

  useEffect(() => {
    if (isCustomer || !role) return;
    partsApi.listParts().then(setCatalog).catch(() => setCatalog([]));
    if (isStaff) usersApi.listTechnicians().then(setTechnicians).catch(() => setTechnicians([]));
  }, [isCustomer, isStaff, role]);

  async function run(key: string, action: () => Promise<void>, fallback: string, success?: string) {
    setActionError(null);
    setNotice(null);
    setBusy(key);
    try {
      await action();
      if (success) setNotice(success);
    } catch (e) {
      setActionError(apiErrorMessage(e, fallback));
    } finally {
      setBusy(null);
    }
  }

  function changeStatus(t: Transition, note?: string) {
    if (!wo) return;
    run(
      `status-${t.to}`,
      async () => {
        setWo(await workOrdersApi.updateWorkOrderStatus(wo.id, { status: t.to, note: note?.trim() || undefined }));
        setPending(null);
        setStatusNote("");
        await refreshLines();
      },
      "Could not update status.",
      `Moved to ${STATUS_LABEL[t.to]}.`
    );
  }

  function assign(e: FormEvent) {
    e.preventDefault();
    if (!wo) return;
    const techId = Number(technicianId);
    if (!techId) return setActionError("Choose a technician.");
    run(
      "assign",
      async () => {
        setWo(await workOrdersApi.assignTechnician(wo.id, { technicianId: techId }));
        setTechnicianId("");
        await refreshLines();
      },
      "Could not assign technician.",
      "Technician assigned and notified."
    );
  }

  function addPart(e: FormEvent) {
    e.preventDefault();
    if (!wo) return;
    const pid = Number(partId);
    const qty = Number(quantity);
    if (!pid || !Number.isInteger(qty) || qty < 1) return setActionError("Choose a part and a whole-number quantity.");
    const stock = catalog.find((p) => p.id === pid)?.stockQuantity;
    if (stock !== undefined && qty > stock) return setActionError(`Only ${stock} in stock.`);
    run(
      "part",
      async () => {
        await workOrdersApi.addPartToWorkOrder(wo.id, { partId: pid, quantity: qty });
        setPartId("");
        setQuantity("1");
        await refreshLines();
        partsApi.listParts().then(setCatalog).catch(() => undefined);
      },
      "Could not log the part.",
      "Part logged and stock updated."
    );
  }

  function logTime(e: FormEvent) {
    e.preventDefault();
    if (!wo) return;
    const total = Number(hours || 0) * 60 + Number(minutes || 0);
    if (!Number.isFinite(total) || total < 1) return setActionError("Enter at least 1 minute.");
    if (total > 24 * 60) return setActionError("A single entry can't exceed 24 hours.");
    run(
      "time",
      async () => {
        await workOrdersApi.logWorkOrderTime(wo.id, { minutes: Math.round(total), description: timeNote.trim() || undefined });
        setHours("0");
        setMinutes("30");
        setTimeNote("");
        await refreshLines();
      },
      "Could not log time.",
      "Time logged."
    );
  }

  function openEdit() {
    if (!wo) return;
    setEdit({
      title: wo.title,
      description: wo.description ?? "",
      siteId: wo.siteId,
      priority: wo.priority,
      slaDueDate: toLocalInput(wo.slaDueDate),
    });
    sitesApi.listSites(wo.customerId).then(setSites).catch(() => setSites([]));
    setEditing(true);
  }

  function saveEdit(e: FormEvent) {
    e.preventDefault();
    if (!wo) return;
    if (!edit.title.trim()) return setActionError("Title is required.");
    run(
      "edit",
      async () => {
        setWo(
          await workOrdersApi.updateWorkOrder(wo.id, {
            title: edit.title.trim(),
            description: edit.description,
            siteId: edit.siteId,
            priority: edit.priority,
            ...(edit.slaDueDate ? { slaDueDate: new Date(edit.slaDueDate).toISOString() } : {}),
          })
        );
        setEditing(false);
        await refreshLines();
      },
      "Could not save changes.",
      "Work order updated."
    );
  }

  const backTo = isStaff ? "/work-orders" : "/dashboard";
  const backLabel = isStaff ? "Work orders" : role === "TECHNICIAN" ? "My jobs" : "My requests";

  if (loading) {
    return (
      <div className="page-stack">
        <section className="panel">
          <div className="wo-card wo-card--skeleton" />
        </section>
      </div>
    );
  }

  if (error || !wo) {
    return (
      <div className="page-stack">
        <section className="panel">
          <div className="form-error">{error ?? "Work order not found."}</div>
          <Link className="btn btn--ghost" to={backTo}>
            ← Back to {backLabel.toLowerCase()}
          </Link>
        </section>
      </div>
    );
  }

  const transitions = allowedTransitions(wo, user);
  const stepIndex = MAIN_PATH.indexOf(wo.status === "ON_HOLD" ? "IN_PROGRESS" : wo.status);

  return (
    <div className="page-stack">
      <div className="page-header">
        <Link className="crumb" to={backTo}>
          ← {backLabel}
        </Link>
        <h1>
          <span className="mono">{woCode(wo)}</span> · {wo.title}
        </h1>
        <div className="header-tags">
          <StatusPill status={wo.status} />
          <PriorityTag priority={wo.priority} />
          <SlaBadge workOrder={wo} />
          {canEdit && !editing && (
            <button className="btn btn--ghost btn--small" onClick={openEdit}>
              ✎ Edit
            </button>
          )}
        </div>
      </div>

      {/* Lifecycle stepper */}
      <section className="panel stepper-panel" aria-label="Lifecycle">
        {wo.status === "CANCELLED" ? (
          <div className="cancelled-banner">
            <strong>Cancelled</strong> — this job was abandoned before completion and can no longer change.
          </div>
        ) : (
          <ol className="stepper">
            {MAIN_PATH.map((s, i) => (
              <li
                key={s}
                className={"stepper__step" + (i < stepIndex ? " is-done" : "") + (i === stepIndex ? " is-current" : "")}
              >
                <span className="stepper__dot" />
                <span className="stepper__label">
                  {i === stepIndex && wo.status === "ON_HOLD" ? "On hold" : STATUS_LABEL[s]}
                </span>
              </li>
            ))}
          </ol>
        )}

        {transitions.length > 0 ? (
          <div className="stepper__actions">
            <span className="muted-note">Next step:</span>
            {transitions.map((t) => (
              <button
                key={t.to}
                className={`btn btn--${t.tone}`}
                disabled={busy !== null}
                onClick={() => (t.needsNote ? (setPending(t), setStatusNote("")) : changeStatus(t))}
              >
                {busy === `status-${t.to}` ? "Saving…" : t.label}
              </button>
            ))}
          </div>
        ) : (
          wo.status !== "CANCELLED" && (
            <p className="muted-note">
              {wo.status === "OPEN"
                ? "Waiting for a technician to be assigned."
                : wo.status === "CLOSED"
                ? "This job is closed and can no longer change."
                : wo.status === "COMPLETED"
                ? "Completed — awaiting sign-off by a manager."
                : "No status actions available for your role."}
            </p>
          )
        )}

        {pending && (
          <form
            className="inline-form note-form"
            onSubmit={(e) => {
              e.preventDefault();
              changeStatus(pending, statusNote);
            }}
          >
            <input
              autoFocus
              placeholder={
                pending.to === "ON_HOLD"
                  ? "Why is it on hold? e.g. waiting for parts"
                  : pending.to === "CANCELLED"
                  ? "Reason for cancelling"
                  : "Reason (optional)"
              }
              value={statusNote}
              onChange={(e) => setStatusNote(e.target.value)}
            />
            <button className={`btn btn--${pending.tone === "danger" ? "danger" : "primary"}`} disabled={busy !== null}>
              Confirm: {pending.label}
            </button>
            <button type="button" className="btn btn--ghost" onClick={() => setPending(null)}>
              Back
            </button>
          </form>
        )}
      </section>

      {actionError && <div className="form-error">{actionError}</div>}
      {notice && <div className="success-note">{notice}</div>}

      {editing && (
        <section className="panel">
          <h2>Edit work order</h2>
          <form className="form-grid" onSubmit={saveEdit}>
            <label className="field field--wide">
              <span>Title</span>
              <input value={edit.title} onChange={(e) => setEdit((f) => ({ ...f, title: e.target.value }))} />
            </label>
            <label className="field field--wide">
              <span>Description</span>
              <textarea rows={2} value={edit.description} onChange={(e) => setEdit((f) => ({ ...f, description: e.target.value }))} />
            </label>
            <label className="field">
              <span>Site</span>
              <select value={edit.siteId} onChange={(e) => setEdit((f) => ({ ...f, siteId: Number(e.target.value) }))}>
                {sites.length === 0 && <option value={wo.siteId}>{wo.siteName}</option>}
                {sites.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Priority</span>
              <select value={edit.priority} onChange={(e) => setEdit((f) => ({ ...f, priority: e.target.value as WorkOrderPriority }))}>
                {PRIORITIES.map((p) => (
                  <option key={p} value={p}>
                    {p.charAt(0) + p.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>SLA due</span>
              <input type="datetime-local" value={edit.slaDueDate} onChange={(e) => setEdit((f) => ({ ...f, slaDueDate: e.target.value }))} />
            </label>
            <div className="form-grid__foot">
              <span className="muted-note">Every change is recorded in the audit trail.</span>
              <div className="inline-form">
                <button type="button" className="btn btn--ghost" onClick={() => setEditing(false)}>
                  Cancel
                </button>
                <button className="btn btn--primary" disabled={busy !== null}>
                  {busy === "edit" ? "Saving…" : "Save changes"}
                </button>
              </div>
            </div>
          </form>
        </section>
      )}

      <div className="split">
        <section className="panel">
          <h2>Job details</h2>
          <div className="profile-grid">
            <div>
              <strong>Customer</strong>
              <div>{wo.customerName}</div>
            </div>
            <div>
              <strong>Site</strong>
              <div>{wo.siteName}</div>
            </div>
            <div>
              <strong>Technician</strong>
              <div>{wo.technicianName ?? "Not assigned"}</div>
            </div>
            <div>
              <strong>SLA due</strong>
              <div>{formatDateTime(wo.slaDueDate)}</div>
            </div>
            <div>
              <strong>Raised</strong>
              <div>{formatDateTime(wo.createdAt)}</div>
            </div>
            <div>
              <strong>{wo.closedAt ? "Closed" : wo.completedAt ? "Completed" : "Last update"}</strong>
              <div>{formatDateTime(wo.closedAt ?? wo.completedAt ?? wo.updatedAt)}</div>
            </div>
          </div>
          <div className="description-block">
            <strong>Description</strong>
            <p>{wo.description || "No description provided."}</p>
          </div>
        </section>

        <section className="panel">
          <h2>{isCustomer ? "Progress" : "Assignment & totals"}</h2>

          {!isCustomer && (
            <div className="totals">
              <div>
                <span className="totals__value">{formatMinutes(totals?.labourMinutes ?? 0)}</span>
                <span className="totals__label">labour logged</span>
              </div>
              <div>
                <span className="totals__value">{formatMoney(totals?.partsCost ?? 0)}</span>
                <span className="totals__label">parts cost ({totals?.partUnits ?? 0} units)</span>
              </div>
              <div>
                <span className="totals__value">{history.length}</span>
                <span className="totals__label">history events</span>
              </div>
            </div>
          )}

          {isCustomer && (
            <p>
              {wo.status === "CANCELLED"
                ? "This request was cancelled."
                : wo.technicianName
                ? `${wo.technicianName} from Meridian is handling this request.`
                : "Meridian's dispatch team will assign a technician shortly."}
            </p>
          )}

          {canAssign && (
            <form className="inline-form" onSubmit={assign}>
              <select value={technicianId} onChange={(e) => setTechnicianId(e.target.value)}>
                <option value="">{wo.technicianId ? "Reassign to…" : "Assign to…"}</option>
                {technicians
                  .filter((t) => t.id !== wo.technicianId)
                  .map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
              </select>
              <button className="btn btn--primary" disabled={busy !== null}>
                {busy === "assign" ? "Assigning…" : wo.technicianId ? "Reassign" : "Assign"}
              </button>
            </form>
          )}
        </section>
      </div>

      {!isCustomer && (
        <div className="split">
          <section className="panel">
            <h2>Parts used</h2>
            {canLogWork && (
              <form className="inline-form" onSubmit={addPart}>
                <select value={partId} onChange={(e) => setPartId(e.target.value)}>
                  <option value="">Choose a part…</option>
                  {catalog.map((p) => (
                    <option key={p.id} value={p.id} disabled={p.stockQuantity <= 0}>
                      {p.name} — {p.stockQuantity} in stock
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  min="1"
                  className="input-narrow"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  aria-label="Quantity"
                />
                <button className="btn btn--primary" disabled={busy !== null}>
                  {busy === "part" ? "Logging…" : "Log part"}
                </button>
              </form>
            )}
            <div className="data-table">
              <table>
                <thead>
                  <tr>
                    <th>Part</th>
                    <th style={{ textAlign: "right" }}>Qty</th>
                    <th style={{ textAlign: "right" }}>Cost</th>
                  </tr>
                </thead>
                <tbody>
                  {lines.length === 0 ? (
                    <tr>
                      <td colSpan={3} className="data-table__status">No parts used yet.</td>
                    </tr>
                  ) : (
                    lines.map((l) => (
                      <tr key={l.id}>
                        <td>
                          <strong>{l.name}</strong>
                          <span className="cell-sub">
                            {l.sku ?? "—"} · {l.loggedByName ?? "—"} · {formatDateTime(l.createdAt)}
                          </span>
                        </td>
                        <td style={{ textAlign: "right" }}>{l.quantity}</td>
                        <td style={{ textAlign: "right" }}>{formatMoney(l.lineCost)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="panel">
            <h2>Time logged</h2>
            {canLogWork && (
              <form className="inline-form" onSubmit={logTime}>
                <input
                  type="number"
                  min="0"
                  max="24"
                  className="input-narrow"
                  value={hours}
                  onChange={(e) => setHours(e.target.value)}
                  aria-label="Hours"
                  title="Hours"
                />
                <span className="muted-note">h</span>
                <input
                  type="number"
                  min="0"
                  max="59"
                  step="5"
                  className="input-narrow"
                  value={minutes}
                  onChange={(e) => setMinutes(e.target.value)}
                  aria-label="Minutes"
                  title="Minutes"
                />
                <span className="muted-note">m</span>
                <input placeholder="What was done (optional)" value={timeNote} onChange={(e) => setTimeNote(e.target.value)} />
                <button className="btn btn--primary" disabled={busy !== null}>
                  {busy === "time" ? "Logging…" : "Log time"}
                </button>
              </form>
            )}
            <div className="data-table">
              <table>
                <thead>
                  <tr>
                    <th>When</th>
                    <th>Note</th>
                    <th style={{ textAlign: "right" }}>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {timeLogs.length === 0 ? (
                    <tr>
                      <td colSpan={3} className="data-table__status">No time logged yet.</td>
                    </tr>
                  ) : (
                    timeLogs.map((t) => (
                      <tr key={t.id}>
                        <td>{formatDateTime(t.createdAt)}</td>
                        <td>
                          {t.description || "—"}
                          {t.loggedByName && <span className="cell-sub">{t.loggedByName}</span>}
                        </td>
                        <td style={{ textAlign: "right" }}>{formatMinutes(t.minutes)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}

      <section className="panel">
        <h2>{isCustomer ? "Request history" : "Audit trail"}</h2>
        {history.length === 0 ? (
          <p className="muted-note">No history recorded.</p>
        ) : (
          <ol className="timeline">
            {history.map((h) => (
              <li key={h.id} className="timeline__item">
                <span className="timeline__dot" />
                <div>
                  <div className="timeline__title">{h.action.replace(/_/g, " ").toLowerCase()}</div>
                  <div className="timeline__body">
                    {h.oldValue && <span className="mono">{h.oldValue}</span>}
                    {h.oldValue && h.newValue && " → "}
                    {h.newValue && <span className="mono">{h.newValue}</span>}
                    {h.note && <div className="timeline__note">“{h.note}”</div>}
                    <div className="cell-sub">by {h.changedByName ?? "System"}</div>
                  </div>
                </div>
                <time className="timeline__time">{formatDateTime(h.createdAt)}</time>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  );
}
