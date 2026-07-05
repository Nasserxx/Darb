import { fetchData, fetchPage, mutateData } from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";

import type {
  AchievementCreateRequest,
  AchievementResponse,
} from "../types/index.ts";

const BASE = "/api/v1/achievements";

export const achievementsApi = {
  getById(id: string) {
    return fetchData<AchievementResponse>(`${BASE}/${id}`);
  },

  listByStudent(studentId: string, params: PageParams = {}) {
    return fetchPage<AchievementResponse>(
      `${BASE}/student/${studentId}`,
      params,
    );
  },

  listByMosque(mosqueId: string, params: PageParams = {}) {
    return fetchPage<AchievementResponse>(
      `${BASE}/mosque/${mosqueId}`,
      params,
    );
  },

  create(body: AchievementCreateRequest) {
    return mutateData<AchievementResponse>(BASE, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },
};
