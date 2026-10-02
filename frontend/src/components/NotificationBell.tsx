import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import * as notificationsApi from "../api/notifications";
import type { AppNotification } from "../types";
import { formatDateTime } from "../lib/workOrders";

const ICON: Record<AppNotification["type"], string> = {
  ASSIGNED: "➜",
  SLA_AT_RISK: "◷",
  SLA_BREACHED: "!",
  STATUS_CHANGED: "↻",
  COMPLETED: "✓",
};

/** In-app notifications (assignment, SLA risk/breach, completion). Polls every minute. */
export default function NotificationBell() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const refreshCount = useCallback(() => {
    notificationsApi.unreadCount().then(setUnread).catch(() => undefined);
  }, []);

  useEffect(() => {
    refreshCount();
    const id = window.setInterval(refreshCount, 60_000);
    return () => window.clearInterval(id);
  }, [refreshCount]);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    notificationsApi
      .listNotifications(false, 15)
      .then((p) => setItems(p.content))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
    const close = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);

  async function openItem(n: AppNotification) {
    if (!n.readAt) {
      notificationsApi.markRead(n.id).catch(() => undefined);
      setUnread((u) => Math.max(0, u - 1));
      setItems((list) => list.map((x) => (x.id === n.id ? { ...x, readAt: new Date().toISOString() } : x)));
    }
    setOpen(false);
    if (n.workOrderId) navigate(`/work-orders/${n.workOrderId}`);
  }

  async function readAll() {
    await notificationsApi.markAllRead().catch(() => undefined);
    setUnread(0);
    setItems((list) => list.map((x) => ({ ...x, readAt: x.readAt ?? new Date().toISOString() })));
  }

  return (
    <div className="bell" ref={ref}>
      <button
        type="button"
        className="bell__button"
        aria-label={`Notifications${unread ? `, ${unread} unread` : ""}`}
        onClick={() => setOpen((v) => !v)}
      >
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
          <path d="M6 16V11a6 6 0 1 1 12 0v5l1.5 2h-15L6 16Z" strokeLinejoin="round" />
          <path d="M10 20a2 2 0 0 0 4 0" strokeLinecap="round" />
        </svg>
        <span className="bell__label">Notifications</span>
        {unread > 0 && <span className="bell__count">{unread > 99 ? "99+" : unread}</span>}
      </button>

      {open && (
        <div className="bell__panel" role="dialog" aria-label="Notifications">
          <div className="bell__head">
            <strong>Notifications</strong>
            {unread > 0 && (
              <button type="button" className="bell__readall" onClick={readAll}>
                Mark all read
              </button>
            )}
          </div>
          {loading ? (
            <p className="muted-note bell__empty">Loading…</p>
          ) : items.length === 0 ? (
            <p className="muted-note bell__empty">You're all caught up.</p>
          ) : (
            <ul className="bell__list">
              {items.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className={`bell__item bell__item--${n.type.toLowerCase()}${n.readAt ? "" : " is-unread"}`}
                    onClick={() => openItem(n)}
                  >
                    <span className="bell__icon" aria-hidden="true">
                      {ICON[n.type]}
                    </span>
                    <span className="bell__text">
                      <span className="bell__title">{n.title}</span>
                      {n.message && <span className="bell__msg">{n.message}</span>}
                      <span className="bell__time">{formatDateTime(n.createdAt)}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
