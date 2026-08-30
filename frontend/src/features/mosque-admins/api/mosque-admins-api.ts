import { apiFetch } from "@/lib/api-client.ts";
import { withAuditReason } from "@/lib/api/audit-reason.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { ApiResponse, PageParams } from "@/lib/types/api.ts";

import type {
  MosqueAdminCreateRequest,
  MosqueAdminResponse,
  MosqueAdminUpdateRequest,
} from "../types/index.ts";
import type { MemberJoinRequestResponse } from "@/features/mosques/types/onboard.ts";

const BASE_PATH = "/api/v1/mosque-admins";

export const mosqueAdminsApi = {
  list: (params: PageParams = {}) =>
    fetchPage<MosqueAdminResponse>(BASE_PATH, params),

  getById: (id: string) =>
    fetchData<MosqueAdminResponse>(`${BASE_PATH}/${id}`),

  create: (body: MosqueAdminCreateRequest, auditReason?: string) =>
    mutateData<MosqueAdminResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
      ...(auditReason
        ? { headers: withAuditReason(undefined, auditReason) }
        : {}),
    }),

  update: (id: string, body: MosqueAdminUpdateRequest, auditReason?: string) =>
    mutateData<MosqueAdminResponse>(`${BASE_PATH}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
      ...(auditReason
        ? { headers: withAuditReason(undefined, auditReason) }
        : {}),
    }),

  delete: async (id: string, auditReason?: string): Promise<void> => {
    await apiFetch<ApiResponse<void>>(`${BASE_PATH}/${id}`, {
      method: "DELETE",
      auth: true,
      ...(auditReason
        ? { headers: withAuditReason(undefined, auditReason) }
        : {}),
    });
  },

  listJoinRequests: async (): Promise<MemberJoinRequestResponse[]> => {
    const response = await apiFetch<ApiResponse<MemberJoinRequestResponse[]>>(
      `${BASE_PATH}/join-requests`,
      { auth: true },
    );
    return response.data ?? [];
  },

  approveJoinRequest: (id: string, auditReason?: string) =>
    mutateData<MemberJoinRequestResponse>(
      `${BASE_PATH}/join-requests/${id}/approve`,
      {
        method: "POST",
        ...(auditReason
          ? { headers: withAuditReason(undefined, auditReason) }
          : {}),
      },
    ),

  rejectJoinRequest: (id: string, auditReason?: string) =>
    mutateData<MemberJoinRequestResponse>(
      `${BASE_PATH}/join-requests/${id}/reject`,
      {
        method: "POST",
        ...(auditReason
          ? { headers: withAuditReason(undefined, auditReason) }
          : {}),
      },
    ),
};
