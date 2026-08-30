import { apiFetch } from "@/lib/api-client.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { MemberJoinRequestResponse } from "@/features/mosques/types/onboard.ts";
import type { ApiResponse, PageParams } from "@/lib/types/api.ts";
import type {
  StudentCreateRequest,
  StudentProvisionRequest,
  StudentResponse,
  StudentUpdateRequest,
} from "../types/index.ts";

const BASE_PATH = "/api/v1/students";

export const studentsApi = {
  list: (params: PageParams = {}) =>
    fetchPage<StudentResponse>(BASE_PATH, params),

  getById: (id: string) => fetchData<StudentResponse>(`${BASE_PATH}/${id}`),

  create: (body: StudentCreateRequest) =>
    mutateData<MemberJoinRequestResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  provision: (body: StudentProvisionRequest) =>
    mutateData<StudentResponse>(`${BASE_PATH}/provision`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  join: (body: { inviteCode: string }) =>
    mutateData<StudentResponse>(`${BASE_PATH}/join`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  onboard: (mosqueId: string) =>
    mutateData<StudentResponse>(`${BASE_PATH}/onboard`, {
      method: "POST",
      body: JSON.stringify({ mosqueId }),
    }),

  update: (id: string, body: StudentUpdateRequest) =>
    mutateData<StudentResponse>(`${BASE_PATH}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  regenerateParentInviteCode: (id: string) =>
    mutateData<StudentResponse>(`${BASE_PATH}/${id}/parent-invite-code`, {
      method: "POST",
    }),

  delete: async (id: string): Promise<void> => {
    await apiFetch<ApiResponse<void>>(`${BASE_PATH}/${id}`, {
      method: "DELETE",
      auth: true,
    });
  },
};
