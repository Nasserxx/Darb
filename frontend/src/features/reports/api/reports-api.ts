import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";
import type {
  ReportCreateRequest,
  ReportResponse,
} from "../types/index.ts";

const BASE = "/api/v1/reports";

export function getReport(id: string) {
  return fetchData<ReportResponse>(`${BASE}/${id}`);
}

export function getMosqueReports(mosqueId: string, params?: PageParams) {
  return fetchPage<ReportResponse>(`${BASE}/mosque/${mosqueId}`, params);
}

export function createReport(body: ReportCreateRequest) {
  return mutateData<ReportResponse>(BASE, {
    method: "POST",
    body: JSON.stringify(body),
  });
}
