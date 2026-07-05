import { apiFetch } from "@/lib/api-client.ts";
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

  search: async (q: string, city?: string): Promise<MosqueSearchResult[]> => {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (city) params.set("city", city);
    const response = await apiFetch<ApiResponse<MosqueSearchResult[]>>(
      `${BASE_PATH}/search?${params.toString()}`,
      { auth: true },
    );
    return response.data ?? [];
  },

  createJoinRequest: (mosqueId: string) =>
    mutateData<MemberJoinRequestResponse>(`${BASE_PATH}/join-requests`, {
      method: "POST",
      body: JSON.stringify({ mosqueId }),
    }),

  getInviteCodes: () =>
    fetchData<MosqueInviteCodesResponse>(`${BASE_PATH}/invite-codes`),

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
