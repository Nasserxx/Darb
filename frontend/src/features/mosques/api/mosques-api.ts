import { apiFetch } from "@/lib/api-client.ts";
import { withAuditReason } from "@/lib/api/audit-reason.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { ApiResponse, PageParams } from "@/lib/types/api.ts";

import type {
  MosqueCreateRequest,
  MosqueResponse,
  MosqueUpdateRequest,
} from "../types/index.ts";
import type {
  MemberJoinRequestResponse,
  MosqueInviteCodesResponse,
  MosqueJoinPreviewResponse,
  MosqueJoinRequest,
  MosqueOnboardResponse,
  MosqueSearchResult,
} from "../types/onboard.ts";

const BASE_PATH = "/api/v1/mosques";

export const mosquesApi = {
  list: (params: PageParams = {}) =>
    fetchPage<MosqueResponse>(BASE_PATH, params),

  listCities: (country: string, opts?: { activeOnly?: boolean }) => {
    const params = new URLSearchParams({ country });
    if (opts?.activeOnly) {
      params.set("activeOnly", "true");
    }
    return fetchData<string[]>(`${BASE_PATH}/cities?${params.toString()}`);
  },

  getById: (id: string) => fetchData<MosqueResponse>(`${BASE_PATH}/${id}`),

  create: (body: MosqueCreateRequest) =>
    mutateData<MosqueResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  onboard: (body: MosqueCreateRequest) =>
    mutateData<MosqueOnboardResponse>(`${BASE_PATH}/onboard`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  join: (body: MosqueJoinRequest) =>
    mutateData<MosqueOnboardResponse>(`${BASE_PATH}/join`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  previewJoin: async (code: string): Promise<MosqueJoinPreviewResponse> => {
    const params = new URLSearchParams({ code });
    return fetchData<MosqueJoinPreviewResponse>(
      `${BASE_PATH}/join/preview?${params.toString()}`,
    );
  },

  previewMemberJoin: async (
    code: string,
    role: "teacher" | "student",
  ): Promise<MosqueJoinPreviewResponse> => {
    const params = new URLSearchParams({ code, role: role.toUpperCase() });
    return fetchData<MosqueJoinPreviewResponse>(
      `${BASE_PATH}/member-join/preview?${params.toString()}`,
    );
  },

  search: (params: PageParams = {}) =>
    fetchPage<MosqueSearchResult>(`${BASE_PATH}/search`, params),

  createJoinRequest: (mosqueId: string) =>
    mutateData<MemberJoinRequestResponse>(`${BASE_PATH}/join-requests`, {
      method: "POST",
      body: JSON.stringify({ mosqueId }),
    }),

  listMyInvitations: () =>
    fetchData<MemberJoinRequestResponse[]>(`${BASE_PATH}/join-requests/mine`),

  acceptJoinRequest: (id: string, auditReason?: string) =>
    mutateData<MemberJoinRequestResponse>(
      `${BASE_PATH}/join-requests/${id}/accept`,
      {
        method: "POST",
        ...(auditReason
          ? { headers: withAuditReason(undefined, auditReason) }
          : {}),
      },
    ),

  refuseJoinRequest: (id: string, auditReason?: string) =>
    mutateData<MemberJoinRequestResponse>(
      `${BASE_PATH}/join-requests/${id}/refuse`,
      {
        method: "POST",
        ...(auditReason
          ? { headers: withAuditReason(undefined, auditReason) }
          : {}),
      },
    ),

  cancelMyJoinRequest: async (): Promise<void> => {
    await apiFetch<ApiResponse<void>>(`${BASE_PATH}/join-requests/my`, {
      method: "DELETE",
      auth: true,
    });
  },

  getInviteCodes: () =>
    fetchData<MosqueInviteCodesResponse>(`${BASE_PATH}/invite-codes`),

  getInviteCodesForMosque: (id: string) =>
    fetchData<MosqueInviteCodesResponse>(`${BASE_PATH}/${id}/invite-codes`),

  rotateInviteCodes: (id: string) =>
    mutateData<MosqueInviteCodesResponse>(
      `${BASE_PATH}/${id}/invite-codes/rotate`,
      { method: "POST" },
    ),

  reactivate: async (id: string): Promise<void> => {
    await apiFetch<ApiResponse<void>>(`${BASE_PATH}/${id}/reactivate`, {
      method: "POST",
      auth: true,
    });
  },

  update: (id: string, body: MosqueUpdateRequest) =>
    mutateData<MosqueResponse>(`${BASE_PATH}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  delete: async (id: string): Promise<void> => {
    await apiFetch<ApiResponse<void>>(`${BASE_PATH}/${id}`, {
      method: "DELETE",
      auth: true,
    });
  },
};
