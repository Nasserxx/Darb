import { apiFetch } from "@/lib/api-client.ts";
import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type {
  ApiResponse,
  PageParams,
  PageResponse,
} from "@/lib/types/api.ts";

import type {
  UserPickerParams,
  UserPickerResponse,
  UserResponse,
  UserUpdateRequest,
} from "../types/index.ts";

const BASE_PATH = "/api/v1/users";

export interface UserSearchParams extends PageParams {
  q: string;
}

export const usersApi = {
  list: (params: PageParams = {}) =>
    fetchPage<UserResponse>(BASE_PATH, params),

  picker: async (params: UserPickerParams) => {
    const search = new URLSearchParams();
    if (params.q) search.set("q", params.q);
    if (params.country) search.set("country", params.country);
    if (params.state) search.set("state", params.state);
    if (params.city) search.set("city", params.city);
    if (params.dateOfBirth) search.set("dateOfBirth", params.dateOfBirth);
    if (params.mosqueId) search.set("mosqueId", params.mosqueId);
    if (params.role) search.set("role", params.role);
    if (params.page !== undefined) search.set("page", String(params.page));
    if (params.size !== undefined) search.set("size", String(params.size));
    const url = `${BASE_PATH}/picker?${search.toString()}`;
    const response = await apiFetch<ApiResponse<PageResponse<UserPickerResponse>>>(
      url,
      { auth: true },
    );
    if (!response.data) throw new Error("Empty picker response");
    return response.data;
  },

  pickerStates: (country: string) =>
    fetchData<string[]>(
      `${BASE_PATH}/picker/states?country=${encodeURIComponent(country)}`,
    ),

  pickerCities: (country: string, state?: string) => {
    const search = new URLSearchParams();
    search.set("country", country);
    if (state) search.set("state", state);
    return fetchData<string[]>(`${BASE_PATH}/picker/cities?${search.toString()}`);
  },

  search: async (params: UserSearchParams) => {
    const search = new URLSearchParams();
    search.set("q", params.q);
    if (params.page !== undefined) search.set("page", String(params.page));
    if (params.size !== undefined) search.set("size", String(params.size));
    if (params.sort) search.set("sort", params.sort);
    const url = `${BASE_PATH}/search?${search.toString()}`;
    const response = await apiFetch<ApiResponse<PageResponse<UserResponse>>>(url, { auth: true });
    if (!response.data) throw new Error("Empty search response");
    return response.data;
  },

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
