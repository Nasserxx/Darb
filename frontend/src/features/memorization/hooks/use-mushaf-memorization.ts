import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
  type UseQueryOptions,
} from "@tanstack/react-query";

import { mushafMemorizationApi } from "../api/mushaf-memorization-api.ts";
import type {
  CoverageGrain,
  CoverageResponse,
  LessonAssignmentResponse,
  LessonAssignmentUpsertRequest,
  MemorizationAttemptCreateRequest,
  MemorizationAttemptResponse,
} from "../types/index.ts";
import { memorizationKeys } from "./memorization-keys.ts";

export function useMushafMetadata(
  options?: Omit<
    UseQueryOptions<Awaited<ReturnType<typeof mushafMemorizationApi.fetchMushafMetadata>>>,
    "queryKey" | "queryFn"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.mushafMetadata(),
    queryFn: () => mushafMemorizationApi.fetchMushafMetadata(),
    staleTime: 24 * 60 * 60 * 1000,
    ...options,
  });
}

export function useJuzMetadata(
  juz: number | undefined,
  options?: Omit<
    UseQueryOptions<Awaited<ReturnType<typeof mushafMemorizationApi.fetchJuzMetadata>>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.juzMetadata(juz ?? 0),
    queryFn: () => mushafMemorizationApi.fetchJuzMetadata(juz!),
    enabled: !!juz && juz >= 1 && juz <= 30,
    staleTime: 24 * 60 * 60 * 1000,
    ...options,
  });
}

export function usePageMetadata(
  page: number | undefined,
  options?: Omit<
    UseQueryOptions<Awaited<ReturnType<typeof mushafMemorizationApi.fetchPageMetadata>>>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.pageMetadata(page ?? 0),
    queryFn: () => mushafMemorizationApi.fetchPageMetadata(page!),
    enabled: !!page && page >= 1 && page <= 604,
    staleTime: 24 * 60 * 60 * 1000,
    ...options,
  });
}

export function useMemorizationCoverage(
  studentId: string | undefined,
  circleId: string | undefined,
  grain: CoverageGrain,
  options?: Omit<
    UseQueryOptions<CoverageResponse>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.coverage(studentId ?? "", circleId ?? "", grain),
    queryFn: () =>
      mushafMemorizationApi.fetchCoverage(studentId!, circleId!, grain),
    enabled: !!studentId && !!circleId,
    ...options,
  });
}

export function useMemorizationAttempts(
  studentId: string | undefined,
  params: { circleId?: string; juz?: number; page?: number } = {},
  options?: Omit<
    UseQueryOptions<MemorizationAttemptResponse[]>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.attempts(studentId ?? "", params),
    queryFn: () => mushafMemorizationApi.fetchAttempts(studentId!, params),
    enabled: !!studentId && !!params.circleId,
    ...options,
  });
}

export function useMemorizationAttempt(
  studentId: string | undefined,
  page: number | undefined,
  half: string | undefined,
  circleId: string | undefined,
  options?: Omit<
    UseQueryOptions<MemorizationAttemptResponse | null>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.attempt(
      studentId ?? "",
      page ?? 0,
      half ?? "",
      circleId ?? "",
    ),
    queryFn: () =>
      mushafMemorizationApi.fetchAttempt(studentId!, page!, half!, circleId!),
    enabled: !!studentId && !!page && !!half && !!circleId,
    ...options,
  });
}

export function useLessonAssignment(
  studentId: string | undefined,
  circleId: string | undefined,
  options?: Omit<
    UseQueryOptions<LessonAssignmentResponse | null>,
    "queryKey" | "queryFn" | "enabled"
  >,
) {
  return useQuery({
    queryKey: memorizationKeys.lesson(studentId ?? "", circleId ?? ""),
    queryFn: () => mushafMemorizationApi.fetchLesson(studentId!, circleId!),
    enabled: !!studentId && !!circleId,
    ...options,
  });
}

export function useCreateMemorizationAttempt(
  studentId: string,
  options?: UseMutationOptions<
    MemorizationAttemptResponse,
    Error,
    MemorizationAttemptCreateRequest
  >,
) {
  const queryClient = useQueryClient();
  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: (body) => mushafMemorizationApi.createAttempt(studentId, body),
    onSuccess: (...args) => {
      const [, variables] = args;
      void queryClient.invalidateQueries({ queryKey: memorizationKeys.all });
      void queryClient.invalidateQueries({
        queryKey: memorizationKeys.attempt(
          studentId,
          variables.page,
          variables.half,
          variables.circleId,
        ),
      });
      return userOnSuccess?.(...args);
    },
  });
}

export function useUpsertLessonAssignment(
  studentId: string,
  options?: UseMutationOptions<
    LessonAssignmentResponse,
    Error,
    LessonAssignmentUpsertRequest
  >,
) {
  const queryClient = useQueryClient();
  const userOnSuccess = options?.onSuccess;

  return useMutation({
    ...options,
    mutationFn: (body) => mushafMemorizationApi.upsertLesson(studentId, body),
    onSuccess: (...args) => {
      const [, variables] = args;
      void queryClient.invalidateQueries({
        queryKey: memorizationKeys.lesson(studentId, variables.circleId),
      });
      return userOnSuccess?.(...args);
    },
  });
}
