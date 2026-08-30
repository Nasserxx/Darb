import type { ApiResponse, PageParams, PageResponse } from "../types/api.ts";
import { apiFetch } from "../api-client.ts";

export function buildPageUrl(path: string, params: PageParams = {}): string {
  const search = new URLSearchParams();
  if (params.page !== undefined) {
    search.set("page", String(params.page));
  }
  if (params.size !== undefined) {
    search.set("size", String(params.size));
  }
  if (params.sort) {
    search.set("sort", params.sort);
  }
  if (params.q) {
    search.set("q", params.q);
  }
  if (params.country) {
    search.set("country", params.country);
  }
  if (params.city) {
    search.set("city", params.city);
  }
  if (params.mosqueId) {
    search.set("mosqueId", params.mosqueId);
  }
  if (params.status) {
    search.set("status", params.status);
  }
  const query = search.toString();
  return query ? `${path}?${query}` : path;
}

export async function fetchPage<T>(
  path: string,
  params: PageParams = {},
): Promise<PageResponse<T>> {
  const response = await apiFetch<ApiResponse<PageResponse<T>>>(
    buildPageUrl(path, params),
    { auth: true },
  );
  if (!response.data) {
    throw new Error("Empty page response");
  }
  return response.data;
}

export async function fetchData<T>(path: string): Promise<T> {
  const response = await apiFetch<ApiResponse<T>>(path, { auth: true });
  if (!response.data) {
    throw new Error("Empty response");
  }
  return response.data;
}

export async function mutateData<T>(
  path: string,
  init: RequestInit,
): Promise<T> {
  const response = await apiFetch<ApiResponse<T>>(path, {
    auth: true,
    ...init,
  });
  if (!response.data) {
    throw new Error("Empty response");
  }
  return response.data;
}
