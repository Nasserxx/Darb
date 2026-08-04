import { apiFetch } from "@/lib/api-client.ts";
import type { ApiResponse } from "@/lib/types/api.ts";

const BASE_PATH = "/api/v1/admin/stuck-work";

export type StuckWorkItemKind = "PENDING_JOIN";

export interface StuckWorkItem {
  kind: StuckWorkItemKind;
  mosqueId: string;
  mosqueName: string;
  resourceId: string;
  summary: string;
  createdAt: string;
  densityScore: number;
}

export const stuckWorkApi = {
  list: async (): Promise<StuckWorkItem[]> => {
    const response = await apiFetch<ApiResponse<StuckWorkItem[]>>(BASE_PATH, {
      auth: true,
    });
    return response.data ?? [];
  },
};
