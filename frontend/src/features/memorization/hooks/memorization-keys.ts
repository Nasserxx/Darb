import type { PageParams } from "@/lib/types/api.ts";
import type { CoverageGrain } from "../types/index.ts";

export const memorizationKeys = {
  all: ["memorization"] as const,
  details: () => [...memorizationKeys.all, "detail"] as const,
  detail: (id: string) => [...memorizationKeys.details(), id] as const,
  students: () => [...memorizationKeys.all, "student"] as const,
  student: (studentId: string, params: PageParams) =>
    [...memorizationKeys.students(), studentId, params] as const,
  circles: () => [...memorizationKeys.all, "circle"] as const,
  circle: (circleId: string, params: PageParams) =>
    [...memorizationKeys.circles(), circleId, params] as const,
  mushaf: () => [...memorizationKeys.all, "mushaf"] as const,
  mushafMetadata: () => [...memorizationKeys.mushaf(), "metadata"] as const,
  juzMetadata: (juz: number) =>
    [...memorizationKeys.mushaf(), "juz", juz] as const,
  pageMetadata: (page: number) =>
    [...memorizationKeys.mushaf(), "page", page] as const,
  coverage: (studentId: string, circleId: string, grain: CoverageGrain) =>
    [...memorizationKeys.all, "coverage", studentId, circleId, grain] as const,
  attempts: (
    studentId: string,
    params: { circleId?: string; juz?: number; page?: number },
  ) => [...memorizationKeys.all, "attempts", studentId, params] as const,
  attempt: (
    studentId: string,
    page: number,
    half: string,
    circleId: string,
  ) =>
    [...memorizationKeys.all, "attempt", studentId, page, half, circleId] as const,
  lesson: (studentId: string, circleId: string) =>
    [...memorizationKeys.all, "lesson", studentId, circleId] as const,
};
