import { apiFetch } from "@/lib/api-client.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { ApiResponse, PageParams } from "@/lib/types/api.ts";
import type {
  CircleCreateRequest,
  CircleResponse,
  CircleUpdateRequest,
} from "../types/index.ts";

const BASE_PATH = "/api/v1/circles";

export const circlesApi = {
  list: (params: PageParams = {}) =>
    fetchPage<CircleResponse>(BASE_PATH, params),

  getById: (id: string) => fetchData<CircleResponse>(`${BASE_PATH}/${id}`),

  create: (body: CircleCreateRequest) =>
    mutateData<CircleResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  update: (id: string, body: CircleUpdateRequest) =>
    mutateData<CircleResponse>(`${BASE_PATH}/${id}`, {
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
