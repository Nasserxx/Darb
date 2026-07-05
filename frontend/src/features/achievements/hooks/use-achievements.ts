import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from "@tanstack/react-query";

import type { PageParams, PageResponse } from "@/lib/types/api.ts";

import { achievementsApi } from "../api/achievements-api.ts";
import type {
  AchievementCreateRequest,
  AchievementResponse,
} from "../types/index.ts";
import { achievementKeys } from "./achievement-keys.ts";

export function useAchievement(
  id: string | undefined,
  options?: Omit<
    UseQueryOptions<AchievementResponse>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: achievementKeys.detail(id ?? ""),
    queryFn: () => achievementsApi.getById(id!),
    enabled: !!id,
    ...options,
  });
}

export function useAchievementsByStudent(
  studentId: string | undefined,
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<AchievementResponse>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: achievementKeys.student(studentId ?? "", params),
    queryFn: () => achievementsApi.listByStudent(studentId!, params),
    enabled: !!studentId,
    ...options,
  });
}

export function useAchievementsByMosque(
  mosqueId: string | undefined,
  params: PageParams = {},
  options?: Omit<
    UseQueryOptions<PageResponse<AchievementResponse>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: achievementKeys.mosque(mosqueId ?? "", params),
    queryFn: () => achievementsApi.listByMosque(mosqueId!, params),
    enabled: !!mosqueId,
    ...options,
  });
}

export function useCreateAchievement(
  options?: UseMutationOptions<
    AchievementResponse,
    Error,
    AchievementCreateRequest
  >,
) {
  const queryClient = useQueryClient();

  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: achievementsApi.create,
    onSuccess: (...args) => {
      void queryClient.invalidateQueries({ queryKey: achievementKeys.all });
      return userOnSuccess?.(...args);
    },
  });
}
