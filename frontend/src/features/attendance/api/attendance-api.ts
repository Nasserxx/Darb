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

  create(body: AttendanceCreateRequest) {
    return mutateData<AttendanceResponse>(BASE, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  update(id: string, body: AttendanceUpdateRequest) {
    return mutateData<AttendanceResponse>(`${BASE}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    });
  },
};
