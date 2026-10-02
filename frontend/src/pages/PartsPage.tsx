import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import * as partsApi from "../api/parts";
import { apiErrorMessage } from "../api/client";
import StatCard from "../components/StatCard";
import { useAuth } from "../context/AuthContext";
import type { Part } from "../types";
import { formatMoney } from "../lib/workOrders";

const LOW_STOCK = 5;

/** Parts inventory. Managers add parts; dispatchers can look up stock. */
export default function PartsPage() {
  const { user } = useAuth();
  const canEdit = user?.role === "MANAGER";

  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [onlyLow, setOnlyLow] = useState(false);

  const [name, setName] = useState("");
  const [stock, setStock] = useState("10");
  const [sku, setSku] = useState("");
  const [unitCost, setUnitCost] = useState("");
  const [restockId, setRestockId] = useState<number | null>(null);
  const [restockQty, setRestockQty] = useState("10");
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setParts(await partsApi.listParts());
    } catch (err) {
      setError(apiErrorMessage(err, "Could not load parts."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    const qty = Number(stock);
    if (!name.trim()) return setFormError("Part name is required.");
    if (!Number.isInteger(qty) || qty < 0) return setFormError("Stock must be a whole number, 0 or more.");
    setSaving(true);
    try {
      const cost = unitCost.trim() === "" ? null : Number(unitCost);
      if (cost !== null && (!Number.isFinite(cost) || cost < 0)) {
        setSaving(false);
        return setFormError("Unit cost must be a number, 0 or more.");
      }
      const created = await partsApi.createPart({
        name: name.trim(),
        stockQuantity: qty,
        sku: sku.trim() || undefined,
        unitCost: cost,
      });
      setParts((list) => [...list, created]);
      setName("");
      setStock("10");
      setSku("");
      setUnitCost("");
    } catch (err) {
      setFormError(apiErrorMessage(err, "Could not add the part."));
    } finally {
      setSaving(false);
    }
  }

  async function restock(id: number) {
    const qty = Number(restockQty);
    if (!Number.isInteger(qty) || qty < 1) return setError("Restock quantity must be at least 1.");
    try {
      const updated = await partsApi.restockPart(id, qty);
      setParts((list) => list.map((p) => (p.id === id ? updated : p)));
      setRestockId(null);
      setRestockQty("10");
    } catch (err) {
      setError(apiErrorMessage(err, "Could not restock the part."));
    }
  }

  const shown = useMemo(() => {
    const q = search.trim().toLowerCase();
    return parts
      .filter((p) => (!q || p.name.toLowerCase().includes(q)) && (!onlyLow || p.stockQuantity <= LOW_STOCK))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [parts, search, onlyLow]);

  const low = parts.filter((p) => p.stockQuantity > 0 && p.stockQuantity <= LOW_STOCK).length;
  const out = parts.filter((p) => p.stockQuantity <= 0).length;
  const maxStock = Math.max(1, ...parts.map((p) => p.stockQuantity));

  return (
    <div className="page-stack">
      <div className="page-header">
        <h1>Parts inventory</h1>
        <p className="page-header__subtitle">
          Stock on hand. Logging a part on a job decrements stock in the same transaction, and stock
          can never go below zero.
        </p>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="stat-grid">
        <StatCard label="Part types" value={loading ? "—" : parts.length} />
        <StatCard label="Stock value" value={loading ? "—" : formatMoney(parts.reduce((s, p) => s + p.stockQuantity * (p.unitCost ?? 0), 0))} hint={`${parts.reduce((s, p) => s + p.stockQuantity, 0)} units`} />
        <StatCard label="Low stock" value={loading ? "—" : low} hint={`${LOW_STOCK} or fewer left`} />
        <StatCard label="Out of stock" value={loading ? "—" : out} />
      </div>

      {canEdit && (
        <section className="panel">
          <h2>Add a part</h2>
          <form className="inline-form" onSubmit={submit}>
            <input placeholder="Part name, e.g. 20A MCB breaker" value={name} onChange={(e) => setName(e.target.value)} />
            <input placeholder="SKU (optional)" value={sku} onChange={(e) => setSku(e.target.value)} style={{ flex: "0 1 160px" }} />
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="Unit cost ₹"
              value={unitCost}
              onChange={(e) => setUnitCost(e.target.value)}
              style={{ flex: "0 1 140px" }}
            />
            <input
              type="number"
              min="0"
              className="input-narrow"
              value={stock}
              onChange={(e) => setStock(e.target.value)}
              aria-label="Opening stock"
            />
            <button className="btn btn--primary" disabled={saving}>
              {saving ? "Adding…" : "Add part"}
            </button>
          </form>
          {formError && <div className="form-error">{formError}</div>}
        </section>
      )}

      <section className="panel">
        <div className="panel__toolbar">
          <h2>All parts</h2>
          <div className="inline-form">
            <input className="search-input" placeholder="Search parts…" value={search} onChange={(e) => setSearch(e.target.value)} />
            <label className="toggle">
              <input type="checkbox" checked={onlyLow} onChange={(e) => setOnlyLow(e.target.checked)} />
              Low stock only
            </label>
          </div>
        </div>

        <div className="data-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Part</th>
                <th style={{ textAlign: "right" }}>Unit cost</th>
                <th>Stock level</th>
                <th style={{ textAlign: "right" }}>On hand</th>
                {canEdit && <th />}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={canEdit ? 6 : 5} className="data-table__status">Loading…</td>
                </tr>
              ) : shown.length === 0 ? (
                <tr>
                  <td colSpan={canEdit ? 6 : 5} className="data-table__status">
                    {parts.length === 0 ? "No parts yet." : "No parts match."}
                  </td>
                </tr>
              ) : (
                shown.map((p) => {
                  const state = p.stockQuantity <= 0 ? "out" : p.stockQuantity <= LOW_STOCK ? "low" : "ok";
                  return (
                    <tr key={p.id}>
                      <td>{p.id}</td>
                      <td>
                        <strong>{p.name}</strong>
                        {p.sku && <span className="cell-sub">{p.sku}</span>}
                      </td>
                      <td style={{ textAlign: "right" }}>{formatMoney(p.unitCost)}</td>
                      <td>
                        <div className="meter" title={`${p.stockQuantity} on hand`}>
                          <span
                            className={`meter__fill meter__fill--${state}`}
                            style={{ width: `${Math.max(2, (p.stockQuantity / maxStock) * 100)}%` }}
                          />
                        </div>
                      </td>
                      <td style={{ textAlign: "right" }}>
                        <span className={`stock stock--${state}`}>
                          {state === "out" ? "Out" : state === "low" ? "Low · " : ""}
                          {state !== "out" && p.stockQuantity}
                        </span>
                      </td>
                      {canEdit && (
                        <td style={{ textAlign: "right" }}>
                          {restockId === p.id ? (
                            <span className="inline-form" style={{ justifyContent: "flex-end", flexWrap: "nowrap" }}>
                              <input
                                type="number"
                                min="1"
                                className="input-narrow"
                                value={restockQty}
                                onChange={(e) => setRestockQty(e.target.value)}
                                aria-label="Quantity received"
                              />
                              <button className="btn btn--primary btn--small" onClick={() => restock(p.id)}>
                                Add
                              </button>
                              <button className="btn btn--ghost btn--small" onClick={() => setRestockId(null)}>
                                ✕
                              </button>
                            </span>
                          ) : (
                            <button className="btn btn--secondary btn--small" onClick={() => setRestockId(p.id)}>
                              + Restock
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

      </section>
    </div>
  );
}
