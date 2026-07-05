import { apiFetch } from "@/lib/api-client.ts";
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

  create: (body: MosqueAdminCreateRequest) =>
    mutateData<MosqueAdminResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  update: (id: string, body: MosqueAdminUpdateRequest) =>
    mutateData<MosqueAdminResponse>(`${BASE_PATH}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  delete: async (id: string): Promise<void> => {
    await apiFetch<ApiResponse<void>>(`${BASE_PATH}/${id}`, {
      method: "DELETE",
      auth: true,
    });
  },

  listJoinRequests: async (): Promise<MemberJoinRequestResponse[]> => {
    const response = await apiFetch<ApiResponse<MemberJoinRequestResponse[]>>(
      `${BASE_PATH}/join-requests`,
      { auth: true },
    );
    return response.data ?? [];
  },

  approveJoinRequest: (id: string) =>
    mutateData<MemberJoinRequestResponse>(
      `${BASE_PATH}/join-requests/${id}/approve`,
      { method: "POST" },
    ),

  rejectJoinRequest: (id: string) =>
    mutateData<MemberJoinRequestResponse>(
      `${BASE_PATH}/join-requests/${id}/reject`,
      { method: "POST" },
    ),
};
