import { fetchPage, mutateData } from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";
import type {
  MessageCreateRequest,
  MessageResponse,
} from "../types/index.ts";

const BASE = "/api/v1/messages";

export function getMyMessages(params?: PageParams) {
  return fetchPage<MessageResponse>(`${BASE}/mine`, params);
}

export function getCircleMessages(circleId: string, params?: PageParams) {
  return fetchPage<MessageResponse>(`${BASE}/circle/${circleId}`, params);
}

export function createMessage(body: MessageCreateRequest) {
  return mutateData<MessageResponse>(BASE, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function markMessageAsRead(id: string) {
  return mutateData<MessageResponse>(`${BASE}/${id}/read`, {
    method: "PUT",
  });
}
