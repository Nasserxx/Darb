import { fetchData, fetchPage, mutateData } from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";

import type {
  GoalCreateRequest,
  GoalResponse,
  GoalUpdateRequest,
} from "../types/index.ts";

const BASE = "/api/v1/goals";

export const goalsApi = {
  getById(id: string) {
    return fetchData<GoalResponse>(`${BASE}/${id}`);
  },

  listByStudent(studentId: string, params: PageParams = {}) {
    return fetchPage<GoalResponse>(`${BASE}/student/${studentId}`, params);
  },

  create(body: GoalCreateRequest) {
    return mutateData<GoalResponse>(BASE, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  update(id: string, body: GoalUpdateRequest) {
    return mutateData<GoalResponse>(`${BASE}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    });
  },
};
