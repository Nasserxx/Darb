import { apiFetch } from "@/lib/api-client.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { ApiResponse, PageParams } from "@/lib/types/api.ts";

import type { UserResponse, UserUpdateRequest } from "../types/index.ts";

const BASE_PATH = "/api/v1/users";

export const usersApi = {
  list: (params: PageParams = {}) =>
    fetchPage<UserResponse>(BASE_PATH, params),

  getById: (id: string) => fetchData<UserResponse>(`${BASE_PATH}/${id}`),

  getMe: () => fetchData<UserResponse>(`${BASE_PATH}/me`),

  updateMe: (body: UserUpdateRequest) =>
    mutateData<UserResponse>(`${BASE_PATH}/me`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  update: (id: string, body: UserUpdateRequest) =>
    mutateData<UserResponse>(`${BASE_PATH}/${id}`, {
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
