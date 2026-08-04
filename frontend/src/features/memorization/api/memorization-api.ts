import { fetchData, fetchPage, mutateData } from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";

import type {
  MemorizationProgressCreateRequest,
  MemorizationProgressResponse,
  MemorizationProgressUpdateRequest,
} from "../types/index.ts";

const BASE = "/api/v1/memorization";

export const memorizationApi = {
  getById(id: string) {
    return fetchData<MemorizationProgressResponse>(`${BASE}/${id}`);
  },

  listByStudent(studentId: string, params: PageParams = {}) {
    return fetchPage<MemorizationProgressResponse>(
      `${BASE}/student/${studentId}`,
      params,
    );
  },

  listByCircle(circleId: string, params: PageParams = {}) {
    return fetchPage<MemorizationProgressResponse>(
      `${BASE}/circle/${circleId}`,
      params,
    );
  },

  create(body: MemorizationProgressCreateRequest) {
    return mutateData<MemorizationProgressResponse>(BASE, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  update(id: string, body: MemorizationProgressUpdateRequest) {
    return mutateData<MemorizationProgressResponse>(`${BASE}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    });
  },
};
