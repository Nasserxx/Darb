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
    AttendanceCreateRequest
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: attendanceApi.create,
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
    { id: string; body: AttendanceUpdateRequest }
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: ({ id, body }) => attendanceApi.update(id, body),
    onSuccess: (...args) => {
      const [, variables] = args;
      queryClient.setQueryData(attendanceKeys.detail(variables.id), args[0]);
      void queryClient.invalidateQueries({ queryKey: attendanceKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}
