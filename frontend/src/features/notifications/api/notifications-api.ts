import { fetchPage, mutateData } from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";
import type {
  NotificationCreateRequest,
  NotificationResponse,
} from "../types/index.ts";

const BASE = "/api/v1/notifications";

export function getMyNotifications(params?: PageParams) {
  return fetchPage<NotificationResponse>(`${BASE}/mine`, params);
}

export function markNotificationAsRead(id: string) {
  return mutateData<NotificationResponse>(`${BASE}/${id}/read`, {
    method: "PUT",
  });
}

export function createNotification(body: NotificationCreateRequest) {
  return mutateData<NotificationResponse>(BASE, {
    method: "POST",
    body: JSON.stringify(body),
  });
}
