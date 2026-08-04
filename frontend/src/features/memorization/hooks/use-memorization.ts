import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from "@tanstack/react-query";

import type { PageParams, PageResponse } from "@/lib/types/api.ts";

import { memorizationApi } from "../api/memorization-api.ts";
import type {
  MemorizationProgressCreateRequest,
  MemorizationProgressResponse,
  MemorizationProgressUpdateRequest,
} from "../types/index.ts";
import { memorizationKeys } from "./memorization-keys.ts";

export function useMemorization(
  id: string | undefined,
  options?: Omit<
    UseQueryOptions<MemorizationProgressResponse>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.detail(id ?? ""),
    queryFn: () => memorizationApi.getById(id!),
    enabled: !!id,
    ...options,
  });
}

export function useMemorizationByStudent(
  studentId: string | undefined,
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<MemorizationProgressResponse>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.student(studentId ?? "", params),
    queryFn: () => memorizationApi.listByStudent(studentId!, params),
    enabled: !!studentId,
    ...options,
  });
}

export function useMemorizationByCircle(
  circleId: string | undefined,
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<MemorizationProgressResponse>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.circle(circleId ?? "", params),
    queryFn: () => memorizationApi.listByCircle(circleId!, params),
    enabled: !!circleId,
    ...options,
  });
}

export function useCreateMemorization(
  options?: UseMutationOptions<
    MemorizationProgressResponse,
    Error,
    MemorizationProgressCreateRequest
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: memorizationApi.create,
    onSuccess: (...args) => {
      void queryClient.invalidateQueries({ queryKey: memorizationKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}

export function useUpdateMemorization(
  options?: UseMutationOptions<
    MemorizationProgressResponse,
    Error,
    { id: string; body: MemorizationProgressUpdateRequest }
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: ({ id, body }) => memorizationApi.update(id, body),
    onSuccess: (...args) => {
      const [, variables] = args;
      queryClient.setQueryData(memorizationKeys.detail(variables.id), args[0]);
      void queryClient.invalidateQueries({ queryKey: memorizationKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}
