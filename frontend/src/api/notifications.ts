import { apiClient } from "./client";
import type { AppNotification, Page } from "../types";

export function listNotifications(unreadOnly = false, size = 15) {
  return apiClient
    .get<Page<AppNotification>>("/api/notifications", { params: { unreadOnly, size } })
    .then((r) => r.data);
}

export function unreadCount() {
  return apiClient.get<{ unread: number }>("/api/notifications/unread-count").then((r) => r.data.unread);
}

export function markRead(id: number) {
  return apiClient.post<AppNotification>(`/api/notifications/${id}/read`).then((r) => r.data);
}

export function markAllRead() {
  return apiClient.post<{ updated: number }>("/api/notifications/read-all").then((r) => r.data);
}
