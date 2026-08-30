import { apiFetch } from "@/lib/api-client.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { MemberJoinRequestResponse } from "@/features/mosques/types/onboard.ts";
import type { ApiResponse, PageParams } from "@/lib/types/api.ts";

import type {
  TeacherCreateRequest,
  TeacherProvisionRequest,
  TeacherResponse,
  TeacherUpdateRequest,
} from "../types/index.ts";

const BASE_PATH = "/api/v1/teachers";

export const teachersApi = {
  list: (params: PageParams = {}) =>
    fetchPage<TeacherResponse>(BASE_PATH, params),

  getById: (id: string) => fetchData<TeacherResponse>(`${BASE_PATH}/${id}`),

  create: (body: TeacherCreateRequest) =>
    mutateData<MemberJoinRequestResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  provision: (body: TeacherProvisionRequest) =>
    mutateData<TeacherResponse>(`${BASE_PATH}/provision`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  join: (body: { inviteCode: string }) =>
    mutateData<TeacherResponse>(`${BASE_PATH}/join`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  onboard: (mosqueId: string) =>
    mutateData<TeacherResponse>(`${BASE_PATH}/onboard`, {
      method: "POST",
      body: JSON.stringify({ mosqueId }),
    }),

  update: (id: string, body: TeacherUpdateRequest) =>
    mutateData<TeacherResponse>(`${BASE_PATH}/${id}`, {
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
