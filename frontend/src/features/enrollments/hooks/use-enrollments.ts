import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import { enrollmentsApi } from "../api/enrollments-api.ts";
import type {
  EnrollmentCreateRequest,
  EnrollmentUpdateRequest,
} from "../types/index.ts";
import { enrollmentKeys } from "./query-keys.ts";

export function useEnrollments(
  params: PageParams = {},
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: enrollmentKeys.list(params),
    queryFn: () => enrollmentsApi.list(params),
    enabled: options?.enabled ?? true,
  });
}

export function useStudentEnrollments(
  studentId: string | undefined,
  params: PageParams = {},
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: enrollmentKeys.student(studentId ?? "", params),
    queryFn: () => enrollmentsApi.listByStudent(studentId!, params),
    enabled: !!studentId && (options?.enabled ?? true),
  });
}

export function useEnrollment(id: string, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: enrollmentKeys.detail(id),
    queryFn: () => enrollmentsApi.getById(id),
    enabled: options?.enabled ?? Boolean(id),
  });
}

export function useCreateEnrollment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      body,
      auditReason,
    }: {
      body: EnrollmentCreateRequest;
      auditReason?: string;
    }) => enrollmentsApi.create(body, auditReason),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: enrollmentKeys.lists(),
      });
    },
  });
}

export function useUpdateEnrollment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      body,
      auditReason,
    }: {
      id: string;
      body: EnrollmentUpdateRequest;
      auditReason?: string;
    }) => enrollmentsApi.update(id, body, auditReason),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({
        queryKey: enrollmentKeys.lists(),
      });
      void queryClient.invalidateQueries({
        queryKey: enrollmentKeys.detail(id),
      });
    },
  });
}
