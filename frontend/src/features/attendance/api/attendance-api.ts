import { withAuditReason } from "@/lib/api/audit-reason.ts";
import { fetchData, fetchPage, mutateData } from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";

import type {
  AttendanceCreateRequest,
  AttendanceResponse,
  AttendanceUpdateRequest,
} from "../types/index.ts";

const BASE = "/api/v1/attendance";

export const attendanceApi = {
  list(params: PageParams = {}) {
    return fetchPage<AttendanceResponse>(BASE, params);
  },

  listByCircle(circleId: string, params: PageParams = {}) {
    return fetchPage<AttendanceResponse>(`${BASE}/circle/${circleId}`, params);
  },

  getById(id: string) {
    return fetchData<AttendanceResponse>(`${BASE}/${id}`);
  },

  listByStudent(studentId: string, params: PageParams = {}) {
    return fetchPage<AttendanceResponse>(`${BASE}/student/${studentId}`, params);
  },

  create(body: AttendanceCreateRequest, auditReason?: string) {
    return mutateData<AttendanceResponse>(BASE, {
      method: "POST",
      body: JSON.stringify(body),
      ...(auditReason
        ? { headers: withAuditReason(undefined, auditReason) }
        : {}),
    });
  },

  update(id: string, body: AttendanceUpdateRequest, auditReason?: string) {
    return mutateData<AttendanceResponse>(`${BASE}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
      ...(auditReason
        ? { headers: withAuditReason(undefined, auditReason) }
        : {}),
    });
  },

  submitExcuse(
    id: string,
    body: { absenceReason?: string; excuseDocumentUrl?: string },
  ) {
    return mutateData<AttendanceResponse>(`${BASE}/${id}/excuse`, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },
};
