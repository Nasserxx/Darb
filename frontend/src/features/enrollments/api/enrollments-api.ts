import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";
import type {
  EnrollmentCreateRequest,
  EnrollmentResponse,
  EnrollmentUpdateRequest,
} from "../types/index.ts";

const BASE_PATH = "/api/v1/enrollments";

export const enrollmentsApi = {
  list: (params: PageParams = {}) =>
    fetchPage<EnrollmentResponse>(BASE_PATH, params),

  getById: (id: string) =>
    fetchData<EnrollmentResponse>(`${BASE_PATH}/${id}`),

  create: (body: EnrollmentCreateRequest) =>
    mutateData<EnrollmentResponse>(BASE_PATH, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  update: (id: string, body: EnrollmentUpdateRequest) =>
    mutateData<EnrollmentResponse>(`${BASE_PATH}/${id}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
};
