import { apiFetch } from "@/lib/api-client.ts";
import { withAuditReason } from "@/lib/api/audit-reason.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { MemberJoinRequestResponse } from "@/features/mosques/types/onboard.ts";
import type { StudentResponse } from "@/features/students/types/index.ts";
import type { ApiResponse, PageParams } from "@/lib/types/api.ts";
import type {
  ParentStudentCreateRequest,
  ParentStudentResponse,
  ParentStudentUpdateRequest,
} from "../types/index.ts";

const BASE_PATH = "/api/v1/parent-students";

export interface ParentStudentJoinPreview {
  mosqueName: string;
  studentName: string;
}

export const parentStudentsApi = {
  list: (params: PageParams = {}) =>
    fetchPage<ParentStudentResponse>(BASE_PATH, params),

  getById: (id: string) =>
    fetchData<ParentStudentResponse>(`${BASE_PATH}/${id}`),

  create: (body: ParentStudentCreateRequest, auditReason?: string) =>
    mutateData<MemberJoinRequestResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
      ...(auditReason
        ? { headers: withAuditReason(undefined, auditReason) }
        : {}),
    }),

  update: (id: string, body: ParentStudentUpdateRequest, auditReason?: string) =>
    mutateData<ParentStudentResponse>(`${BASE_PATH}/${id}`, {
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

  previewJoin: async (code: string): Promise<ParentStudentJoinPreview> => {
    const params = new URLSearchParams({ code });
    return fetchData<ParentStudentJoinPreview>(
      `${BASE_PATH}/join/preview?${params.toString()}`,
    );
  },

  linkByInviteCode: (body: { inviteCode: string }) =>
    mutateData<ParentStudentResponse>(`${BASE_PATH}/join`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  getMyChildren: () =>
    fetchData<StudentResponse[]>(`${BASE_PATH}/my-children`),
};
