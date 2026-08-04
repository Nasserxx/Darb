import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from "@tanstack/react-query";

import type { PageParams, PageResponse } from "@/lib/types/api.ts";

import { goalsApi } from "../api/goals-api.ts";
import type {
  GoalCreateRequest,
  GoalResponse,
  GoalUpdateRequest,
} from "../types/index.ts";
import { goalKeys } from "./goal-keys.ts";

export function useGoal(
  id: string | undefined,
  options?: Omit<
    UseQueryOptions<GoalResponse>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: goalKeys.detail(id ?? ""),
    queryFn: () => goalsApi.getById(id!),
    enabled: !!id,
    ...options,
  });
}

export function useGoalsByStudent(
  studentId: string | undefined,
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<GoalResponse>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: goalKeys.student(studentId ?? "", params),
    queryFn: () => goalsApi.listByStudent(studentId!, params),
    enabled: !!studentId,
    ...options,
  });
}

export function useCreateGoal(
  options?: UseMutationOptions<GoalResponse, Error, GoalCreateRequest>,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: goalsApi.create,
    onSuccess: (...args) => {
      void queryClient.invalidateQueries({ queryKey: goalKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}

export function useUpdateGoal(
  options?: UseMutationOptions<
    GoalResponse,
    Error,
    { id: string; body: GoalUpdateRequest }
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: ({ id, body }) => goalsApi.update(id, body),
    onSuccess: (...args) => {
      const [, variables] = args;
      queryClient.setQueryData(goalKeys.detail(variables.id), args[0]);
      void queryClient.invalidateQueries({ queryKey: goalKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}
