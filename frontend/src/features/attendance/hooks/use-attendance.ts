import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from "@tanstack/react-query";

import type { PageParams, PageResponse } from "@/lib/types/api.ts";

import { attendanceApi } from "../api/attendance-api.ts";
import type {
  AttendanceCreateRequest,
  AttendanceResponse,
  AttendanceUpdateRequest,
} from "../types/index.ts";
import { attendanceKeys } from "./attendance-keys.ts";

export function useAttendanceList(
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<AttendanceResponse>>,
    "queryKey" | "queryFn"
  >,
) {
  return useQuery({
    queryKey: attendanceKeys.list(params),
    queryFn: () => attendanceApi.list(params),
    ...options,
  });
}

export function useAttendanceByCircle(
  circleId: string | undefined,
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<AttendanceResponse>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: attendanceKeys.circle(circleId ?? "", params),
    queryFn: () => attendanceApi.listByCircle(circleId!, params),
    enabled: !!circleId,
    ...options,
  });
}

export function useAttendanceByStudent(
  studentId: string | undefined,
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<AttendanceResponse>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: attendanceKeys.student(studentId ?? "", params),
    queryFn: () => attendanceApi.listByStudent(studentId!, params),
    enabled: !!studentId,
    ...options,
  });
}

export function useAttendance(
  id: string | undefined,
  options?: Omit<
    UseQueryOptions<AttendanceResponse>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: attendanceKeys.detail(id ?? ""),
    queryFn: () => attendanceApi.getById(id!),
    enabled: !!id,
    ...options,
  });
}

export function useCreateAttendance(
  options?: UseMutationOptions<
    AttendanceResponse,
    Error,
    { body: AttendanceCreateRequest; auditReason?: string }
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: ({ body, auditReason }) =>
      attendanceApi.create(body, auditReason),
    onSuccess: (...args) => {
      void queryClient.invalidateQueries({ queryKey: attendanceKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}

export function useUpdateAttendance(
  options?: UseMutationOptions<
    AttendanceResponse,
    Error,
    { id: string; body: AttendanceUpdateRequest; auditReason?: string }
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: ({ id, body, auditReason }) =>
      attendanceApi.update(id, body, auditReason),
    onSuccess: (...args) => {
      const [, variables] = args;
      queryClient.setQueryData(attendanceKeys.detail(variables.id), args[0]);
      void queryClient.invalidateQueries({ queryKey: attendanceKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}

export function useSubmitExcuse(
  options?: UseMutationOptions<
    AttendanceResponse,
    Error,
    { id: string; body: { absenceReason?: string; excuseDocumentUrl?: string } }
  >,
) {
  const queryClient = useQueryClient();
  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: ({ id, body }) => attendanceApi.submitExcuse(id, body),
    onSuccess: (...args) => {
      const [, variables] = args;
      queryClient.setQueryData(attendanceKeys.detail(variables.id), args[0]);
      void queryClient.invalidateQueries({ queryKey: attendanceKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}
